package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.CompanyEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.repository.CompanyEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmotionalAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(EmotionalAggregationService.class);
    private static final int MIN_USERS_FOR_AGGREGATION = 5;

    private final EmotionalAnalysisRepository emotionalAnalysisRepository;
    private final DepartmentEmotionalAggregateRepository departmentAggregateRepository;
    private final CompanyEmotionalAggregateRepository companyAggregateRepository;

    public EmotionalAggregationService(
            EmotionalAnalysisRepository emotionalAnalysisRepository,
            DepartmentEmotionalAggregateRepository departmentAggregateRepository,
            CompanyEmotionalAggregateRepository companyAggregateRepository) {
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
        this.departmentAggregateRepository = departmentAggregateRepository;
        this.companyAggregateRepository = companyAggregateRepository;
    }

    @Transactional
    public DepartmentEmotionalAggregate aggregateByDepartment(UUID departmentId, LocalDate date) {
        logger.info("Agregando dados emocionais para departamento {} na data {}", departmentId, date);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
            departmentId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            logger.warn("Nenhuma análise encontrada para departamento {} na data {}", departmentId, date);
            return null;
        }

        long uniqueUsers = analyses.stream()
            .map(EmotionalAnalysis::getUsuarioId)
            .distinct()
            .count();

        if (uniqueUsers < MIN_USERS_FOR_AGGREGATION) {
            logger.warn("Departamento {} não atende k-anonymity ({} usuários < {} mínimo) na data {}",
                departmentId, uniqueUsers, MIN_USERS_FOR_AGGREGATION, date);
            return null;
        }

        UUID companyId = analyses.stream()
            .map(EmotionalAnalysis::getCompanyId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (companyId == null) {
            logger.warn("CompanyId não encontrado para departamento {}", departmentId);
            return null;
        }

        double avgStressLevel = calculateAvgStressLevel(analyses);
        double avgEmotionalIntensity = calculateAvgEmotionalIntensity(analyses);
        Map<String, Integer> primaryEmotions = calculatePrimaryEmotions(analyses);
        long totalConversations = analyses.stream()
            .map(EmotionalAnalysis::getConversationId)
            .distinct()
            .count();
        long totalMessages = analyses.size();
        long riskAlertsCount = countRiskAlerts(analyses);

        DepartmentEmotionalAggregate aggregate = departmentAggregateRepository
            .findByDepartmentIdAndDate(departmentId, date)
            .orElse(new DepartmentEmotionalAggregate());

        aggregate.setDepartmentId(departmentId);
        aggregate.setCompanyId(companyId);
        aggregate.setDate(date);
        aggregate.setAvgStressLevel(avgStressLevel);
        aggregate.setAvgEmotionalIntensity(avgEmotionalIntensity);
        aggregate.setPrimaryEmotions(primaryEmotions);
        aggregate.setTotalConversations(totalConversations);
        aggregate.setTotalMessages(totalMessages);
        aggregate.setRiskAlertsCount(riskAlertsCount);
        aggregate.setUniqueUsersCount(uniqueUsers);

        DepartmentEmotionalAggregate saved = departmentAggregateRepository.save(aggregate);
        logger.info("Agregação de departamento salva: {} - {} usuários, {} conversas, stress médio: {}",
            departmentId, uniqueUsers, totalConversations, avgStressLevel);

        return saved;
    }

    @Transactional
    public CompanyEmotionalAggregate aggregateByCompany(UUID companyId, LocalDate date) {
        logger.info("Agregando dados emocionais para empresa {} na data {}", companyId, date);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByCompanyIdAndDateRange(
            companyId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            logger.warn("Nenhuma análise encontrada para empresa {} na data {}", companyId, date);
            return null;
        }

        double avgStressLevel = calculateAvgStressLevel(analyses);
        long totalActiveUsers = analyses.stream()
            .map(EmotionalAnalysis::getUsuarioId)
            .distinct()
            .count();
        long totalConversations = analyses.stream()
            .map(EmotionalAnalysis::getConversationId)
            .distinct()
            .count();
        long totalMessages = analyses.size();
        long riskAlertsCount = countRiskAlerts(analyses);

        Map<String, Object> departmentBreakdown = analyses.stream()
            .filter(a -> a.getDepartmentId() != null)
            .collect(Collectors.groupingBy(
                a -> a.getDepartmentId().toString(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    deptAnalyses -> {
                        Map<String, Object> deptData = new HashMap<>();
                        deptData.put("avgStressLevel", calculateAvgStressLevel(deptAnalyses));
                        deptData.put("avgIntensity", calculateAvgEmotionalIntensity(deptAnalyses));
                        deptData.put("totalConversations", deptAnalyses.stream()
                            .map(EmotionalAnalysis::getConversationId)
                            .distinct()
                            .count());
                        deptData.put("totalMessages", (long) deptAnalyses.size());
                        return deptData;
                    }
                )
            ));

        CompanyEmotionalAggregate aggregate = companyAggregateRepository
            .findByCompanyIdAndDate(companyId, date)
            .orElse(new CompanyEmotionalAggregate());

        aggregate.setCompanyId(companyId);
        aggregate.setDate(date);
        aggregate.setAvgStressLevel(avgStressLevel);
        aggregate.setDepartmentBreakdown(departmentBreakdown);
        aggregate.setTotalActiveUsers(totalActiveUsers);
        aggregate.setTotalConversations(totalConversations);
        aggregate.setTotalMessages(totalMessages);
        aggregate.setRiskAlertsCount(riskAlertsCount);

        CompanyEmotionalAggregate saved = companyAggregateRepository.save(aggregate);
        logger.info("Agregação de empresa salva: {} - {} usuários, {} conversas, stress médio: {}",
            companyId, totalActiveUsers, totalConversations, avgStressLevel);

        return saved;
    }

    @Transactional
    public void aggregateByDepartmentForDateRange(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            aggregateByDepartment(departmentId, currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }

    @Transactional
    public void aggregateByCompanyForDateRange(UUID companyId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            aggregateByCompany(companyId, currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }

    @Transactional
    public void processNewEmotionalAnalysis(EmotionalAnalysis analysis) {
        if (analysis.getDepartmentId() == null || analysis.getCompanyId() == null) {
            logger.warn("Análise emocional sem department_id ou company_id, pulando agregação em tempo real");
            return;
        }

        LocalDate analysisDate = analysis.getCreatedAt().toLocalDate();

        aggregateByDepartment(analysis.getDepartmentId(), analysisDate);

        aggregateByCompany(analysis.getCompanyId(), analysisDate);
    }

    private double calculateAvgStressLevel(List<EmotionalAnalysis> analyses) {
        return analyses.stream()
            .mapToInt(EmotionalAnalysis::getIntensity)
            .average()
            .orElse(0.0);
    }

    private double calculateAvgEmotionalIntensity(List<EmotionalAnalysis> analyses) {
        return analyses.stream()
            .mapToInt(EmotionalAnalysis::getIntensity)
            .average()
            .orElse(0.0);
    }

    private Map<String, Integer> calculatePrimaryEmotions(List<EmotionalAnalysis> analyses) {
        return analyses.stream()
            .collect(Collectors.groupingBy(
                EmotionalAnalysis::getPrimaryEmotional,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    private long countRiskAlerts(List<EmotionalAnalysis> analyses) {

        return analyses.stream()
            .filter(a -> a.getIntensity() != null && a.getIntensity() > 80)
            .count();
    }
}
