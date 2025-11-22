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

    /**
     * Analisa correlação entre gatilhos e emoções.
     * Identifica padrões como: "Conversas com X → ansiedade aumenta 80%"
     */
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

            // Busca todas as mensagens do usuário de uma vez (evita N+1)
            List<ConversationMessage> userMessages = messageRepository
                    .findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);
            
            // Coleta dados de mensagens e emoções
            List<MessageEmotionData> messageEmotions = new ArrayList<>();
            for (ConversationMessage msg : userMessages) {
                // Busca análise emocional associada à mensagem (feita pela IA)
                Optional<EmotionalAnalysis> analysis = emotionalAnalysisRepository
                        .findByMessageId(msg.getId());
                
                messageEmotions.add(new MessageEmotionData(
                        msg.getContent(),
                        analysis.orElse(null)
                ));
            }

            // Para cada gatilho, analisa correlação com emoções
            for (Trigger trigger : triggers) {
                analisarCorrelacaoTrigger(trigger, messageEmotions);
            }

            logger.info("Análise de correlação gatilho-emoção concluída para usuário: {}", userId);
        } catch (Exception e) {
            logger.error("Erro ao analisar correlação gatilho-emoção: {}", e.getMessage(), e);
        }
    }

    /**
     * Analisa correlação de um gatilho específico com emoções.
     */
    private void analisarCorrelacaoTrigger(Trigger trigger, List<MessageEmotionData> messages) {
        // Filtra mensagens que mencionam o gatilho
        List<MessageEmotionData> mensagensComGatilho = messages.stream()
                .filter(msg -> mensagemMencionaGatilho(msg.conteudo, trigger.getDescricao()))
                .collect(Collectors.toList());

        if (mensagensComGatilho.size() < MIN_OBSERVACOES_CORRELACAO) {
            return; // Poucas observações para estabelecer correlação
        }

        // Analisa emoções associadas usando análise emocional da IA
        Map<String, Integer> contagemEmocoes = new HashMap<>();
        Map<String, Integer> somaIntensidades = new HashMap<>();
        Map<String, Integer> contagemIntensidades = new HashMap<>();

        for (MessageEmotionData msg : mensagensComGatilho) {
            String emocao = null;
            Integer intensidade = null;
            
            // Prioriza usar análise emocional da IA se disponível
            if (msg.emocao != null && msg.emocao.getPrimaryEmotional() != null) {
                emocao = msg.emocao.getPrimaryEmotional();
                intensidade = msg.emocao.getIntensity();
            } else {
                // Fallback: infere do texto se não houver análise salva
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

        // Determina emoção mais associada
        String emocaoMaisComum = contagemEmocoes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (emocaoMaisComum != null && !emocaoMaisComum.equals(trigger.getEmocaoAssociada())) {
            trigger.setEmocaoAssociada(emocaoMaisComum);
            triggerRepository.save(trigger);
            logger.info("Emoção associada atualizada para gatilho {}: {}", trigger.getDescricao(), emocaoMaisComum);
        }

        // Calcula impacto baseado na frequência e intensidade média
        double frequenciaRelativa = (double) mensagensComGatilho.size() / messages.size();
        
        // Calcula intensidade média da emoção associada
        double intensidadeMedia = 50.0; // Default
        if (emocaoMaisComum != null && contagemIntensidades.containsKey(emocaoMaisComum)) {
            int soma = somaIntensidades.get(emocaoMaisComum);
            int count = contagemIntensidades.get(emocaoMaisComum);
            intensidadeMedia = (double) soma / count;
        }
        
        // Impacto considera frequência e intensidade (intensidade alta = impacto alto)
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

    /**
     * Verifica se uma mensagem menciona um gatilho.
     */
    private boolean mensagemMencionaGatilho(String mensagem, String descricaoGatilho) {
        String mensagemLower = mensagem.toLowerCase();
        String descricaoLower = descricaoGatilho.toLowerCase();
        
        // Verifica se a descrição do gatilho está contida na mensagem
        // ou se palavras-chave do gatilho aparecem
        return mensagemLower.contains(descricaoLower) ||
               Arrays.stream(descricaoLower.split("\\s+"))
                       .anyMatch(palavra -> palavra.length() > 3 && mensagemLower.contains(palavra));
    }

    /**
     * Infere emoção do texto da mensagem (análise básica de fallback).
     * Usado apenas quando não há EmotionalAnalysis salvo.
     */
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

    /**
     * Calcula impacto baseado na frequência de ocorrência e intensidade emocional.
     */
    private int calcularImpactoBaseadoEmFrequenciaEIntensidade(double frequenciaRelativa, 
                                                               int ocorrencias, 
                                                               double intensidadeMedia) {
        // Impacto baseado em frequência e número de observações
        double impactoBase = frequenciaRelativa * 10; // 0-10
        double bonusObservacoes = Math.min(2.0, Math.log10(ocorrencias)); // Bônus por mais observações
        
        // Ajusta impacto baseado na intensidade emocional (intensidade alta = impacto maior)
        // Normaliza intensidade (0-100) para fator de ajuste (0.5-1.5)
        double fatorIntensidade = 0.5 + (intensidadeMedia / 100.0);
        
        int impacto = (int) Math.round((impactoBase + bonusObservacoes) * fatorIntensidade);
        return Math.max(1, Math.min(10, impacto));
    }

    // Classe auxiliar
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

