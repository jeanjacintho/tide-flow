package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.OllamaClient;
import br.jeanjacintho.tideflow.ai_service.model.Memoria;
import br.jeanjacintho.tideflow.ai_service.model.TipoMemoria;
import br.jeanjacintho.tideflow.ai_service.repository.MemoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class MemoriaService {

    private static final Logger logger = LoggerFactory.getLogger(MemoriaService.class);
    private static final int MAX_MEMORIAS_RELEVANTES = 10;
    private static final int DIAS_PARA_PERGUNTA_PROATIVA = 7;

    private final MemoriaRepository memoriaRepository;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public MemoriaService(MemoriaRepository memoriaRepository, OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.memoriaRepository = memoriaRepository;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa mensagem e resposta da IA para extrair e salvar memórias.
     * Executa de forma assíncrona para não travar a resposta ao usuário.
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processarMensagemParaMemoria(String usuarioId, String userMessage, String aiResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Iniciando extração de memórias para usuário: {}", usuarioId);
                
                String jsonResponse = ollamaClient.extractMemories(userMessage, aiResponse)
                        .block();

                if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                    logger.warn("Resposta vazia do Ollama para extração de memórias");
                    return null;
                }

                // Limpa a resposta se vier com markdown
                jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> memoriasData = (List<Map<String, Object>>) responseMap.get("memorias");

                if (memoriasData == null || memoriasData.isEmpty()) {
                    logger.info("Nenhuma memória extraída da conversa");
                    return null;
                }

                String contextoCompleto = String.format("Usuário: %s\nIA: %s", userMessage, aiResponse);
                int memoriasSalvas = 0;

                for (Map<String, Object> memoriaData : memoriasData) {
                    try {
                        Memoria memoria = criarMemoriaFromData(usuarioId, memoriaData, contextoCompleto);
                        memoriaRepository.save(memoria);
                        memoriasSalvas++;
                        logger.info("Memória salva: {} - {}", memoria.getTipo(), memoria.getConteudo());
                    } catch (Exception e) {
                        logger.error("Erro ao salvar memória individual: {}", e.getMessage(), e);
                    }
                }

                logger.info("Total de memórias salvas: {}", memoriasSalvas);
                return null;
            } catch (Exception e) {
                logger.error("Erro ao processar memórias: {}", e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Recupera memórias relevantes do usuário para incluir no contexto da conversa.
     */
    @Transactional(readOnly = true)
    public String recuperarMemoriasRelevantes(String usuarioId, String mensagemAtual) {
        List<Memoria> memorias = memoriaRepository.findMemoriasRelevantes(usuarioId);
        
        if (memorias.isEmpty()) {
            return "";
        }

        // Filtra e limita as mais relevantes
        List<Memoria> memoriasSelecionadas = memorias.stream()
                .limit(MAX_MEMORIAS_RELEVANTES)
                .collect(Collectors.toList());

        // Atualiza contadores de referência (assíncrono)
        atualizarReferencias(memoriasSelecionadas);

        // Formata memórias para o prompt
        return formatarMemoriasParaPrompt(memoriasSelecionadas);
    }

    /**
     * Formata memórias em texto para incluir no prompt do Ollama.
     */
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

    /**
     * Atualiza contadores de referência das memórias usadas.
     */
    @Async
    @Transactional
    public void atualizarReferencias(List<Memoria> memorias) {
        for (Memoria memoria : memorias) {
            memoria.incrementarReferencia();
            memoriaRepository.save(memoria);
        }
    }

    /**
     * Sugere uma pergunta proativa baseada em memórias antigas não referenciadas.
     */
    @Transactional(readOnly = true)
    public Optional<String> sugerirPerguntaProativa(String usuarioId) {
        java.time.LocalDateTime dataLimite = java.time.LocalDateTime.now()
                .minusDays(DIAS_PARA_PERGUNTA_PROATIVA);

        List<Memoria> memoriasAntigas = memoriaRepository
                .findMemoriasNaoReferenciadasRecentemente(usuarioId, dataLimite);

        if (memoriasAntigas.isEmpty()) {
            return Optional.empty();
        }

        // Pega a memória mais relevante que não foi referenciada recentemente
        Memoria memoria = memoriasAntigas.get(0);

        try {
            String pergunta = ollamaClient.generateProactiveQuestion(
                    memoria.getConteudo(),
                    memoria.getTipo().name()
            ).block();

            if (pergunta != null && !pergunta.trim().isEmpty()) {
                // Atualiza referência da memória usada
                memoria.incrementarReferencia();
                memoriaRepository.save(memoria);
                
                return Optional.of(pergunta);
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar pergunta proativa: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Cria uma entidade Memoria a partir dos dados extraídos pelo Ollama.
     */
    private Memoria criarMemoriaFromData(String usuarioId, Map<String, Object> data, String contexto) {
        String tipoStr = (String) data.get("tipo");
        TipoMemoria tipo;
        try {
            tipo = TipoMemoria.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = TipoMemoria.FATO_PESSOAL; // Default
        }

        String conteudo = (String) data.get("conteudo");
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("Conteúdo da memória não pode ser vazio");
        }

        Integer relevancia = 50; // Default
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

