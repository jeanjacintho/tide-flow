package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.DepartmentHeatmapDTO;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentKeywordAnalysis;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentKeywordAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DepartmentHeatmapService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentHeatmapService.class);
    
    private static final double LOW_STRESS_THRESHOLD = 30.0;
    private static final double MEDIUM_STRESS_THRESHOLD = 50.0;
    private static final double HIGH_STRESS_THRESHOLD = 70.0;

    private final DepartmentEmotionalAggregateRepository departmentAggregateRepository;
    private final DepartmentKeywordAnalysisRepository keywordAnalysisRepository;

    public DepartmentHeatmapService(
            DepartmentEmotionalAggregateRepository departmentAggregateRepository,
            DepartmentKeywordAnalysisRepository keywordAnalysisRepository) {
        this.departmentAggregateRepository = departmentAggregateRepository;
        this.keywordAnalysisRepository = keywordAnalysisRepository;
    }

    /**
     * Obtém mapa de calor por departamento para uma empresa em uma data específica.
     * 
     * @param companyId ID da empresa
     * @param date Data para análise
     * @return DepartmentHeatmapDTO com departamentos e seus níveis de stress
     */
    @Transactional(readOnly = true)
    public DepartmentHeatmapDTO getDepartmentHeatmap(UUID companyId, LocalDate date) {
        logger.info("Obtendo mapa de calor para empresa {} na data {}", companyId, date);
        
        List<DepartmentEmotionalAggregate> aggregates = departmentAggregateRepository.findAllByCompanyIdAndDate(
            companyId, date
        );
        
        if (aggregates.isEmpty()) {
            logger.warn("Nenhuma agregação encontrada para empresa {} na data {}", companyId, date);
            return new DepartmentHeatmapDTO(companyId, date, new ArrayList<>());
        }

        List<DepartmentHeatmapDTO.DepartmentHeatmapItem> departments = new ArrayList<>();
        
        for (DepartmentEmotionalAggregate aggregate : aggregates) {
            String stressColor = determineStressColor(aggregate.getAvgStressLevel());
            
            DepartmentKeywordAnalysis keywordAnalysis = keywordAnalysisRepository
                .findByDepartmentIdAndDate(aggregate.getDepartmentId(), date)
                .orElse(null);
            
            Map<String, Object> topKeywords = keywordAnalysis != null && keywordAnalysis.getKeywords() != null
                ? new HashMap<>(keywordAnalysis.getKeywords())
                : new HashMap<>();
            
            Map<String, Object> topTriggers = keywordAnalysis != null && keywordAnalysis.getTopTriggers() != null
                ? new HashMap<>(keywordAnalysis.getTopTriggers())
                : new HashMap<>();
            
            departments.add(new DepartmentHeatmapDTO.DepartmentHeatmapItem(
                aggregate.getDepartmentId(),
                null, // Nome do departamento seria buscado do user-service
                aggregate.getAvgStressLevel(),
                stressColor,
                aggregate.getUniqueUsersCount(),
                aggregate.getTotalConversations(),
                aggregate.getRiskAlertsCount(),
                topKeywords,
                topTriggers
            ));
        }
        
        return new DepartmentHeatmapDTO(companyId, date, departments);
    }

    /**
     * Obtém insights detalhados de um departamento específico.
     */
    @Transactional(readOnly = true)
    public DepartmentHeatmapDTO.DepartmentHeatmapItem getDepartmentInsights(UUID departmentId, LocalDate date) {
        logger.info("Obtendo insights para departamento {} na data {}", departmentId, date);
        
        DepartmentEmotionalAggregate aggregate = departmentAggregateRepository
            .findByDepartmentIdAndDate(departmentId, date)
            .orElse(null);
        
        if (aggregate == null) {
            logger.warn("Nenhuma agregação encontrada para departamento {} na data {}", departmentId, date);
            return null;
        }
        
        String stressColor = determineStressColor(aggregate.getAvgStressLevel());
        
        DepartmentKeywordAnalysis keywordAnalysis = keywordAnalysisRepository
            .findByDepartmentIdAndDate(departmentId, date)
            .orElse(null);
        
        Map<String, Object> topKeywords = keywordAnalysis != null && keywordAnalysis.getKeywords() != null
            ? new HashMap<>(keywordAnalysis.getKeywords())
            : new HashMap<>();
        
        Map<String, Object> topTriggers = keywordAnalysis != null && keywordAnalysis.getTopTriggers() != null
            ? new HashMap<>(keywordAnalysis.getTopTriggers())
            : new HashMap<>();
        
        return new DepartmentHeatmapDTO.DepartmentHeatmapItem(
            aggregate.getDepartmentId(),
            null, // Nome do departamento seria buscado do user-service
            aggregate.getAvgStressLevel(),
            stressColor,
            aggregate.getUniqueUsersCount(),
            aggregate.getTotalConversations(),
            aggregate.getRiskAlertsCount(),
            topKeywords,
            topTriggers
        );
    }

    /**
     * Determina a cor do stress baseado no nível.
     * Verde (baixo), Amarelo (médio), Laranja (alto), Vermelho (crítico)
     */
    private String determineStressColor(Double stressLevel) {
        if (stressLevel == null) {
            return "GRAY";
        }
        
        if (stressLevel < LOW_STRESS_THRESHOLD) {
            return "GREEN"; // Verde - baixo stress
        } else if (stressLevel < MEDIUM_STRESS_THRESHOLD) {
            return "YELLOW"; // Amarelo - médio stress
        } else if (stressLevel < HIGH_STRESS_THRESHOLD) {
            return "ORANGE"; // Laranja - alto stress
        } else {
            return "RED"; // Vermelho - crítico
        }
    }
}
