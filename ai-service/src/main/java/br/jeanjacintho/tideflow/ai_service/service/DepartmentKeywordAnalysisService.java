package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.DepartmentKeywordAnalysis;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentKeywordAnalysisRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DepartmentKeywordAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentKeywordAnalysisService.class);

    private final DepartmentKeywordAnalysisRepository keywordAnalysisRepository;
    private final KeywordExtractionService keywordExtractionService;
    private final DepartmentTriggerAnalysisService triggerAnalysisService;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public DepartmentKeywordAnalysisService(
            DepartmentKeywordAnalysisRepository keywordAnalysisRepository,
            KeywordExtractionService keywordExtractionService,
            DepartmentTriggerAnalysisService triggerAnalysisService,
            EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.keywordAnalysisRepository = keywordAnalysisRepository;
        this.keywordExtractionService = keywordExtractionService;
        this.triggerAnalysisService = triggerAnalysisService;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Transactional
    public DepartmentKeywordAnalysis analyzeDepartmentKeywordsAndTriggers(UUID departmentId, UUID companyId, LocalDate date) {
        logger.info("Analisando keywords e triggers para departamento {} na data {}", departmentId, date);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        Map<String, Integer> keywords = keywordExtractionService.extractKeywordsFromDepartment(
            departmentId, startDateTime, endDateTime
        );

        Map<String, Object> topTriggers = triggerAnalysisService.aggregateTriggersByDepartment(
            departmentId, startDateTime, endDateTime
        );

        Double sentimentScore = keywordExtractionService.calculateSentimentScore(
            departmentId, startDateTime, endDateTime
        );

        Long totalMessages = emotionalAnalysisRepository.countMessagesByDepartmentAndDate(departmentId, date);

        DepartmentKeywordAnalysis analysis = keywordAnalysisRepository
            .findByDepartmentIdAndDate(departmentId, date)
            .orElse(new DepartmentKeywordAnalysis());

        analysis.setDepartmentId(departmentId);
        analysis.setCompanyId(companyId);
        analysis.setDate(date);

        Map<String, Object> keywordsMap = new HashMap<>();
        keywords.forEach((key, value) -> keywordsMap.put(key, value));
        analysis.setKeywords(keywordsMap);

        analysis.setTopTriggers(topTriggers);
        analysis.setSentimentScore(sentimentScore);
        analysis.setTotalMessagesAnalyzed(totalMessages);

        DepartmentKeywordAnalysis saved = keywordAnalysisRepository.save(analysis);
        logger.info("Análise de keywords e triggers salva para departamento {} na data {}: {} keywords, {} triggers",
            departmentId, date, keywords.size(), topTriggers.size());

        return saved;
    }

    @Transactional
    public void analyzeAllDepartmentsForDate(LocalDate date) {
        logger.info("Iniciando análise de keywords e triggers para todos os departamentos na data {}", date);

        java.util.List<UUID> departmentIds = emotionalAnalysisRepository.findDistinctDepartmentIdsByDate(date);

        if (departmentIds.isEmpty()) {
            logger.info("Nenhum departamento encontrado para análise na data {}", date);
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (UUID departmentId : departmentIds) {
            try {
                List<br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis> analyses =
                    emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
                        departmentId,
                        date.atStartOfDay(),
                        date.atTime(LocalTime.MAX)
                    );

                if (!analyses.isEmpty()) {
                    UUID companyId = analyses.get(0).getCompanyId();
                    if (companyId != null) {
                        analyzeDepartmentKeywordsAndTriggers(departmentId, companyId, date);
                        successCount++;
                    } else {
                        logger.warn("Análise sem companyId para departamento {}", departmentId);
                        errorCount++;
                    }
                } else {
                    logger.debug("Nenhuma análise encontrada para departamento {} na data {}", departmentId, date);
                }
            } catch (Exception e) {
                logger.error("Erro ao analisar keywords e triggers para departamento {}: {}",
                    departmentId, e.getMessage(), e);
                errorCount++;
            }
        }

        logger.info("Análise de keywords e triggers concluída: {} sucessos, {} erros", successCount, errorCount);
    }
}
