package br.jeanjacintho.tideflow.ai_service.scheduler;

import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import br.jeanjacintho.tideflow.ai_service.service.DepartmentKeywordAnalysisService;
import br.jeanjacintho.tideflow.ai_service.service.EmotionalAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class EmotionalAggregationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmotionalAggregationScheduler.class);

    private final EmotionalAggregationService aggregationService;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;
    private final DepartmentKeywordAnalysisService keywordAnalysisService;

    public EmotionalAggregationScheduler(
            EmotionalAggregationService aggregationService,
            EmotionalAnalysisRepository emotionalAnalysisRepository,
            DepartmentKeywordAnalysisService keywordAnalysisService) {
        this.aggregationService = aggregationService;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
        this.keywordAnalysisService = keywordAnalysisService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void aggregatePreviousDay() {
        logger.info("Iniciando agregação diária de dados emocionais");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            logger.info("Agregando dados do dia anterior: {}", yesterday);

            List<UUID> departmentIds = emotionalAnalysisRepository.findDistinctDepartmentIdsByDate(yesterday);
            logger.info("Encontrados {} departamentos para agregação", departmentIds.size());

            for (UUID departmentId : departmentIds) {
                try {
                    aggregationService.aggregateByDepartment(departmentId, yesterday);
                } catch (Exception e) {
                    logger.error("Erro ao agregar dados do departamento {}: {}", departmentId, e.getMessage(), e);
                }
            }

            List<UUID> companyIds = emotionalAnalysisRepository.findDistinctCompanyIdsByDate(yesterday);

            logger.info("Encontradas {} empresas para agregação", companyIds.size());

            for (UUID companyId : companyIds) {
                try {
                    aggregationService.aggregateByCompany(companyId, yesterday);
                } catch (Exception e) {
                    logger.error("Erro ao agregar dados da empresa {}: {}", companyId, e.getMessage(), e);
                }
            }

            logger.info("Iniciando análise de keywords e triggers para o dia anterior");
            try {
                keywordAnalysisService.analyzeAllDepartmentsForDate(yesterday);
            } catch (Exception e) {
                logger.error("Erro ao analisar keywords e triggers: {}", e.getMessage(), e);
            }

            logger.info("Agregação diária de dados emocionais concluída");
        } catch (Exception e) {
            logger.error("Erro na agregação diária: {}", e.getMessage(), e);
        }
    }
}
