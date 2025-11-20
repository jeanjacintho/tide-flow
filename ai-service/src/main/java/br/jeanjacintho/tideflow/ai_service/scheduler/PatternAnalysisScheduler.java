package br.jeanjacintho.tideflow.ai_service.scheduler;

import br.jeanjacintho.tideflow.ai_service.repository.ConversationRepository;
import br.jeanjacintho.tideflow.ai_service.service.PatternAnalysisService;
import br.jeanjacintho.tideflow.ai_service.service.TriggerAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatternAnalysisScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PatternAnalysisScheduler.class);

    private final PatternAnalysisService patternAnalysisService;
    private final TriggerAnalysisService triggerAnalysisService;
    private final ConversationRepository conversationRepository;

    public PatternAnalysisScheduler(PatternAnalysisService patternAnalysisService,
                                   TriggerAnalysisService triggerAnalysisService,
                                   ConversationRepository conversationRepository) {
        this.patternAnalysisService = patternAnalysisService;
        this.triggerAnalysisService = triggerAnalysisService;
        this.conversationRepository = conversationRepository;
    }

    /**
     * Executa análise de padrões diariamente às 2h da manhã.
     * Analisa padrões para todos os usuários que têm conversas.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Todo dia às 2h da manhã
    public void analisarPadroesDiariamente() {
        logger.info("Iniciando análise diária de padrões temporais");
        
        try {
            // Busca todos os userIds únicos que têm conversas
            List<String> userIds = conversationRepository.findAll().stream()
                    .map(conversation -> conversation.getUserId())
                    .distinct()
                    .collect(Collectors.toList());

            logger.info("Encontrados {} usuários para análise de padrões", userIds.size());

            for (String userId : userIds) {
                try {
                    patternAnalysisService.analisarPadroesTemporais(userId);
                    // Também analisa correlação gatilho-emoção
                    triggerAnalysisService.analisarCorrelacaoGatilhoEmocao(userId);
                } catch (Exception e) {
                    logger.error("Erro ao analisar padrões/gatilhos para usuário {}: {}", userId, e.getMessage(), e);
                }
            }

            logger.info("Análise diária de padrões temporais e gatilhos concluída");
        } catch (Exception e) {
            logger.error("Erro na análise diária de padrões: {}", e.getMessage(), e);
        }
    }
}

