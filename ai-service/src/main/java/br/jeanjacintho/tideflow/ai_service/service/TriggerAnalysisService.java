package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import br.jeanjacintho.tideflow.ai_service.repository.TriggerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TriggerAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(TriggerAnalysisService.class);
    private static final int MIN_OBSERVACOES_CORRELACAO = 3;

    private final TriggerRepository triggerRepository;
    private final ConversationMessageRepository messageRepository;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public TriggerAnalysisService(TriggerRepository triggerRepository,
                                 ConversationMessageRepository messageRepository,
                                 EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.triggerRepository = triggerRepository;
        this.messageRepository = messageRepository;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Transactional
    @Async
    public void analisarCorrelacaoGatilhoEmocao(String userId) {
        logger.info("Iniciando análise de correlação gatilho-emoção para usuário: {}", userId);

        try {
            List<Trigger> triggers = triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId);

            if (triggers.isEmpty()) {
                logger.info("Nenhum gatilho encontrado para análise");
                return;
            }

            List<ConversationMessage> userMessages = messageRepository
                    .findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);

            List<MessageEmotionData> messageEmotions = new ArrayList<>();
            for (ConversationMessage msg : userMessages) {

                Optional<EmotionalAnalysis> analysis = emotionalAnalysisRepository
                        .findByMessageId(msg.getId());

                messageEmotions.add(new MessageEmotionData(
                        msg.getContent(),
                        analysis.orElse(null)
                ));
            }

            for (Trigger trigger : triggers) {
                analisarCorrelacaoTrigger(trigger, messageEmotions);
            }

            logger.info("Análise de correlação gatilho-emoção concluída para usuário: {}", userId);
        } catch (Exception e) {
            logger.error("Erro ao analisar correlação gatilho-emoção: {}", e.getMessage(), e);
        }
    }

    private void analisarCorrelacaoTrigger(Trigger trigger, List<MessageEmotionData> messages) {

        List<MessageEmotionData> mensagensComGatilho = messages.stream()
                .filter(msg -> mensagemMencionaGatilho(msg.conteudo, trigger.getDescricao()))
                .collect(Collectors.toList());

        if (mensagensComGatilho.size() < MIN_OBSERVACOES_CORRELACAO) {
            return;
        }

        Map<String, Integer> contagemEmocoes = new HashMap<>();
        Map<String, Integer> somaIntensidades = new HashMap<>();
        Map<String, Integer> contagemIntensidades = new HashMap<>();

        for (MessageEmotionData msg : mensagensComGatilho) {
            String emocao = null;
            Integer intensidade = null;

            if (msg.emocao != null && msg.emocao.getPrimaryEmotional() != null) {
                emocao = msg.emocao.getPrimaryEmotional();
                intensidade = msg.emocao.getIntensity();
            } else {

                emocao = inferirEmocaoDoTexto(msg.conteudo);
            }

            if (emocao != null) {
                contagemEmocoes.put(emocao, contagemEmocoes.getOrDefault(emocao, 0) + 1);

                if (intensidade != null) {
                    somaIntensidades.put(emocao, somaIntensidades.getOrDefault(emocao, 0) + intensidade);
                    contagemIntensidades.put(emocao, contagemIntensidades.getOrDefault(emocao, 0) + 1);
                }
            }
        }

        String emocaoMaisComum = contagemEmocoes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (emocaoMaisComum != null && !emocaoMaisComum.equals(trigger.getEmocaoAssociada())) {
            trigger.setEmocaoAssociada(emocaoMaisComum);
            triggerRepository.save(trigger);
            logger.info("Emoção associada atualizada para gatilho {}: {}", trigger.getDescricao(), emocaoMaisComum);
        }

        double frequenciaRelativa = (double) mensagensComGatilho.size() / messages.size();

        double intensidadeMedia = 50.0;
        if (emocaoMaisComum != null && contagemIntensidades.containsKey(emocaoMaisComum)) {
            int soma = somaIntensidades.get(emocaoMaisComum);
            int count = contagemIntensidades.get(emocaoMaisComum);
            intensidadeMedia = (double) soma / count;
        }

        int novoImpacto = calcularImpactoBaseadoEmFrequenciaEIntensidade(
                frequenciaRelativa,
                mensagensComGatilho.size(),
                intensidadeMedia
        );

        if (novoImpacto != trigger.getImpacto()) {
            trigger.setImpacto(novoImpacto);
            triggerRepository.save(trigger);
        }
    }

    private boolean mensagemMencionaGatilho(String mensagem, String descricaoGatilho) {
        String mensagemLower = mensagem.toLowerCase();
        String descricaoLower = descricaoGatilho.toLowerCase();

        return mensagemLower.contains(descricaoLower) ||
               Arrays.stream(descricaoLower.split("\\s+"))
                       .anyMatch(palavra -> palavra.length() > 3 && mensagemLower.contains(palavra));
    }

    private String inferirEmocaoDoTexto(String texto) {
        String textoLower = texto.toLowerCase();

        if (textoLower.contains("triste") || textoLower.contains("tristeza") ||
            textoLower.contains("deprimido") || textoLower.contains("chateado")) {
            return "tristeza";
        }
        if (textoLower.contains("ansioso") || textoLower.contains("ansiedade") ||
            textoLower.contains("preocupado") || textoLower.contains("nervoso")) {
            return "ansiedade";
        }
        if (textoLower.contains("feliz") || textoLower.contains("alegria") ||
            textoLower.contains("contente") || textoLower.contains("bem")) {
            return "alegria";
        }
        if (textoLower.contains("raiva") || textoLower.contains("irritado") ||
            textoLower.contains("bravo") || textoLower.contains("frustrado")) {
            return "raiva";
        }
        if (textoLower.contains("medo") || textoLower.contains("assustado") ||
            textoLower.contains("apreensivo")) {
            return "medo";
        }

        return null;
    }

    private int calcularImpactoBaseadoEmFrequenciaEIntensidade(double frequenciaRelativa,
                                                               int ocorrencias,
                                                               double intensidadeMedia) {

        double impactoBase = frequenciaRelativa * 10;
        double bonusObservacoes = Math.min(2.0, Math.log10(ocorrencias));

        double fatorIntensidade = 0.5 + (intensidadeMedia / 100.0);

        int impacto = (int) Math.round((impactoBase + bonusObservacoes) * fatorIntensidade);
        return Math.max(1, Math.min(10, impacto));
    }

    private static class MessageEmotionData {
        String conteudo;
        br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis emocao;

        MessageEmotionData(String conteudo,
                          br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis emocao) {
            this.conteudo = conteudo;
            this.emocao = emocao;
        }
    }
}
