package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.DashboardOverviewDTO;
import br.jeanjacintho.tideflow.ai_service.model.CompanyEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentKeywordAnalysis;
import br.jeanjacintho.tideflow.ai_service.repository.CompanyEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentKeywordAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CorporateDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(CorporateDashboardService.class);

    private final CompanyEmotionalAggregateRepository companyAggregateRepository;
    private final DepartmentKeywordAnalysisRepository keywordAnalysisRepository;

    public CorporateDashboardService(
            CompanyEmotionalAggregateRepository companyAggregateRepository,
            DepartmentKeywordAnalysisRepository keywordAnalysisRepository) {
        this.companyAggregateRepository = companyAggregateRepository;
        this.keywordAnalysisRepository = keywordAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewDTO getDashboardOverview(UUID companyId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        logger.info("Obtendo overview do dashboard para empresa {} na data {}", companyId, date);

        CompanyEmotionalAggregate aggregate = companyAggregateRepository
            .findByCompanyIdAndDate(companyId, date)
            .orElse(null);

        if (aggregate == null) {
            logger.warn("Nenhuma agregação encontrada para empresa {} na data {}", companyId, date);
            return createEmptyOverview(companyId, date);
        }

        List<DepartmentKeywordAnalysis> keywordAnalyses = keywordAnalysisRepository
            .findByCompanyIdAndDateRange(companyId, date, date);

        Map<String, Integer> topKeywords = extractTopKeywords(keywordAnalyses);
        Map<String, Object> topTriggers = extractTopTriggers(keywordAnalyses);

        return new DashboardOverviewDTO(
            companyId,
            date,
            aggregate.getAvgStressLevel(),
            aggregate.getAvgStressLevel(),
            aggregate.getTotalActiveUsers(),
            aggregate.getTotalConversations(),
            aggregate.getTotalMessages(),
            aggregate.getRiskAlertsCount(),
            aggregate.getDepartmentBreakdown(),
            topKeywords,
            topTriggers
        );
    }

    private Map<String, Integer> extractTopKeywords(List<DepartmentKeywordAnalysis> keywordAnalyses) {
        Map<String, Integer> allKeywords = new HashMap<>();

        for (DepartmentKeywordAnalysis analysis : keywordAnalyses) {
            if (analysis.getKeywords() != null) {
                for (Map.Entry<String, Object> entry : analysis.getKeywords().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        int count = ((Number) value).intValue();
                        allKeywords.merge(entry.getKey(), count, Integer::sum);
                    }
                }
            }
        }

        return allKeywords.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                HashMap::new
            ));
    }

    private Map<String, Object> extractTopTriggers(List<DepartmentKeywordAnalysis> keywordAnalyses) {
        Map<String, Integer> triggerFrequency = new HashMap<>();

        for (DepartmentKeywordAnalysis analysis : keywordAnalyses) {
            if (analysis.getTopTriggers() != null) {
                for (String trigger : analysis.getTopTriggers().keySet()) {
                    triggerFrequency.merge(trigger, 1, Integer::sum);
                }
            }
        }

        Map<String, Object> topTriggers = new HashMap<>();
        triggerFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> topTriggers.put(entry.getKey(), entry.getValue()));

        return topTriggers;
    }

    private DashboardOverviewDTO createEmptyOverview(UUID companyId, LocalDate date) {
        return new DashboardOverviewDTO(
            companyId,
            date,
            0.0,
            0.0,
            0L,
            0L,
            0L,
            0L,
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>()
        );
    }
}
