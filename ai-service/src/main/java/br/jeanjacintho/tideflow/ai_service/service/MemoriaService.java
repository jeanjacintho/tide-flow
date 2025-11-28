package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
import br.jeanjacintho.tideflow.ai_service.model.Memoria;
import br.jeanjacintho.tideflow.ai_service.model.TipoMemoria;
import br.jeanjacintho.tideflow.ai_service.repository.MemoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class MemoriaService {

    private static final Logger logger = LoggerFactory.getLogger(MemoriaService.class);
    private static final int MAX_MEMORIAS_RELEVANTES = 10;
    private static final int DIAS_PARA_PERGUNTA_PROATIVA = 7;

    private final MemoriaRepository memoriaRepository;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;
    private final TriggerService triggerService;

    public MemoriaService(MemoriaRepository memoriaRepository, LLMClient llmClient,
                         ObjectMapper objectMapper, TriggerService triggerService) {
        this.memoriaRepository = memoriaRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.triggerService = triggerService;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> processarMensagemParaMemoriaConsolidada(
            String usuarioId,
            String userMessage,
            String aiResponse,
            Map<String, Object> responseMap
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Iniciando processamento consolidado de memórias para usuário: {}", usuarioId);

                List<Map<String, Object>> memoriasData = extractMapListFromResponse(responseMap, "memorias");

                if (memoriasData == null || memoriasData.isEmpty()) {
                    logger.info("Nenhuma memória extraída da conversa");

                    triggerService.processarGatilhos(usuarioId, responseMap);
                    return null;
                }

                String contextoCompleto = String.format("Usuário: %s\nIA: %s", userMessage, aiResponse);
                List<Memoria> memoriasParaSalvar = new ArrayList<>();

                for (Map<String, Object> memoriaData : memoriasData) {
                    try {
                        Memoria memoria = criarMemoriaFromData(usuarioId, memoriaData, contextoCompleto);
                        memoriasParaSalvar.add(memoria);
                        logger.info("Memória preparada: {} - {}", memoria.getTipo(), memoria.getConteudo());
                    } catch (Exception e) {
                        logger.error("Erro ao criar memória: {}", e.getMessage(), e);
                    }
                }

                if (!memoriasParaSalvar.isEmpty()) {
                    memoriaRepository.saveAll(memoriasParaSalvar);
                    logger.info("Total de memórias salvas: {}", memoriasParaSalvar.size());

                    invalidarCacheMemorias(usuarioId);
                }

                triggerService.processarGatilhos(usuarioId, responseMap);

                return null;
            } catch (Exception e) {
                logger.error("Erro ao processar memórias consolidadas: {}", e.getMessage(), e);
                return null;
            }
        });
    }

    @Cacheable(value = "memoriasRelevantes", key = "#usuarioId", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public Mono<String> recuperarMemoriasRelevantesAsync(String usuarioId, String mensagemAtual) {
        return Mono.fromCallable(() -> {
            List<Memoria> memorias = memoriaRepository.findMemoriasRelevantes(usuarioId);

            if (memorias.isEmpty()) {
                return "";
            }

            List<Memoria> memoriasSelecionadas = memorias.stream()
                    .limit(MAX_MEMORIAS_RELEVANTES)
                    .collect(Collectors.toList());

            atualizarReferencias(memoriasSelecionadas);

            return formatarMemoriasParaPrompt(memoriasSelecionadas);
        });
    }

    @Cacheable(value = "memoriasRelevantes", key = "#usuarioId", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public String recuperarMemoriasRelevantes(String usuarioId, String mensagemAtual) {
        List<Memoria> memorias = memoriaRepository.findMemoriasRelevantes(usuarioId);

        if (memorias.isEmpty()) {
            return "";
        }

        List<Memoria> memoriasSelecionadas = memorias.stream()
                .limit(MAX_MEMORIAS_RELEVANTES)
                .collect(Collectors.toList());

        atualizarReferencias(memoriasSelecionadas);

        return formatarMemoriasParaPrompt(memoriasSelecionadas);
    }

    @CacheEvict(value = "memoriasRelevantes", key = "#usuarioId")
    public void invalidarCacheMemorias(String usuarioId) {
        logger.debug("Cache de memórias invalidado para usuário: {}", usuarioId);
    }

    private List<Map<String, Object>> extractMapListFromResponse(Map<String, Object> responseMap, String key) {
        Object obj = responseMap.getOrDefault(key, new ArrayList<>());
        if (obj instanceof List<?>) {
            List<?> rawList = (List<?>) obj;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?>) {
                    Map<?, ?> rawMap = (Map<?, ?>) item;
                    Map<String, Object> typedMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            typedMap.put((String) entry.getKey(), entry.getValue());
                        }
                    }
                    result.add(typedMap);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private String formatarMemoriasParaPrompt(List<Memoria> memorias) {
        if (memorias.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== MEMÓRIAS DO USUÁRIO ===\n\n");

        for (Memoria memoria : memorias) {
            sb.append(String.format("- [%s] %s\n", memoria.getTipo().name(), memoria.getConteudo()));
        }

        sb.append("\nUse essas informações para fazer conexões relevantes e mostrar que você se lembra do usuário. ");
        sb.append("Não mencione explicitamente que está consultando memórias, apenas use o contexto naturalmente.\n");

        return sb.toString();
    }

    @Async
    @Transactional
    public void atualizarReferencias(List<Memoria> memorias) {
        for (Memoria memoria : memorias) {
            memoria.incrementarReferencia();
            memoriaRepository.save(memoria);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> sugerirPerguntaProativa(String usuarioId) {
        java.time.LocalDateTime dataLimite = java.time.LocalDateTime.now()
                .minusDays(DIAS_PARA_PERGUNTA_PROATIVA);

        List<Memoria> memoriasAntigas = memoriaRepository
                .findMemoriasNaoReferenciadasRecentemente(usuarioId, dataLimite);

        if (memoriasAntigas.isEmpty()) {
            return Optional.empty();
        }

        Memoria memoria = memoriasAntigas.get(0);

        try {
            String pergunta = llmClient.generateProactiveQuestion(
                    memoria.getConteudo(),
                    memoria.getTipo().name()
            ).block();

            if (pergunta != null && !pergunta.trim().isEmpty()) {

                memoria.incrementarReferencia();
                memoriaRepository.save(memoria);

                return Optional.of(pergunta);
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar pergunta proativa: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    private Memoria criarMemoriaFromData(String usuarioId, Map<String, Object> data, String contexto) {
        String tipoStr = (String) data.get("tipo");
        TipoMemoria tipo;
        try {
            tipo = TipoMemoria.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = TipoMemoria.FATO_PESSOAL;
        }

        String conteudo = (String) data.get("conteudo");
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("Conteúdo da memória não pode ser vazio");
        }

        Integer relevancia = 50;
        Object relevanciaObj = data.get("relevancia");
        if (relevanciaObj instanceof Number) {
            relevancia = ((Number) relevanciaObj).intValue();
        }

        Object tagsObj = data.get("tags");
        if (tagsObj != null) {
            try {
                String tagsJson = objectMapper.writeValueAsString(tagsObj);
                Memoria memoria = new Memoria(usuarioId, conteudo, tipo, contexto, relevancia);
                memoria.setTags(tagsJson);
                return memoria;
            } catch (Exception e) {
                logger.warn("Erro ao serializar tags: {}", e.getMessage());
            }
        }

        return new Memoria(usuarioId, conteudo, tipo, contexto, relevancia);
    }
}
