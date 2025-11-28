package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.ImpactAnalysisDTO;
import br.jeanjacintho.tideflow.ai_service.model.CompanyEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.repository.CompanyEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentEmotionalAggregateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImpactAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImpactAnalysisService.class);

    private static final int DAYS_BEFORE_EVENT = 7;
    private static final int DAYS_AFTER_EVENT = 7;

    private final CompanyEmotionalAggregateRepository companyAggregateRepository;
    private final DepartmentEmotionalAggregateRepository departmentAggregateRepository;

    public ImpactAnalysisService(
            CompanyEmotionalAggregateRepository companyAggregateRepository,
            DepartmentEmotionalAggregateRepository departmentAggregateRepository) {
        this.companyAggregateRepository = companyAggregateRepository;
        this.departmentAggregateRepository = departmentAggregateRepository;
    }

    @Transactional(readOnly = true)
    public ImpactAnalysisDTO analyzeDecisionImpact(UUID companyId, LocalDate eventDate, String eventDescription) {
        logger.info("Analisando impacto de decisão para empresa {} na data {}: {}",
            companyId, eventDate, eventDescription);

        LocalDate beforeStart = eventDate.minusDays(DAYS_BEFORE_EVENT);
        LocalDate beforeEnd = eventDate.minusDays(1);
        LocalDate afterStart = eventDate;
        LocalDate afterEnd = eventDate.plusDays(DAYS_AFTER_EVENT);

        List<CompanyEmotionalAggregate> beforeAggregates = companyAggregateRepository
            .findByCompanyIdAndDateBetween(companyId, beforeStart, beforeEnd);

        List<CompanyEmotionalAggregate> afterAggregates = companyAggregateRepository
            .findByCompanyIdAndDateBetween(companyId, afterStart, afterEnd);

        ImpactAnalysisDTO.ImpactMetrics beforeMetrics = calculateMetrics(beforeAggregates);
        ImpactAnalysisDTO.ImpactMetrics afterMetrics = calculateMetrics(afterAggregates);
        ImpactAnalysisDTO.ImpactChanges changes = calculateChanges(beforeMetrics, afterMetrics);

        List<ImpactAnalysisDTO.DepartmentImpact> departmentImpacts = analyzeDepartmentImpacts(
            companyId, beforeStart, beforeEnd, afterStart, afterEnd
        );

        String overallAssessment = generateOverallAssessment(changes);

        return new ImpactAnalysisDTO(
            companyId,
            eventDate,
            eventDescription,
            beforeMetrics,
            afterMetrics,
            changes,
            departmentImpacts,
            overallAssessment
        );
    }

    private ImpactAnalysisDTO.ImpactMetrics calculateMetrics(List<CompanyEmotionalAggregate> aggregates) {
        if (aggregates.isEmpty()) {
            return new ImpactAnalysisDTO.ImpactMetrics(0.0, 0.0, 50.0, 50.0, 0L, 0L);
        }

        double avgStress = aggregates.stream()
            .filter(a -> a.getAvgStressLevel() != null)
            .mapToDouble(CompanyEmotionalAggregate::getAvgStressLevel)
            .average()
            .orElse(0.0);

        double avgIntensity = aggregates.stream()
            .filter(a -> a.getAvgStressLevel() != null)
            .mapToDouble(CompanyEmotionalAggregate::getAvgStressLevel)
            .average()
            .orElse(0.0);

        long totalConversations = aggregates.stream()
            .filter(a -> a.getTotalConversations() != null)
            .mapToLong(CompanyEmotionalAggregate::getTotalConversations)
            .sum();

        long activeUsers = aggregates.stream()
            .filter(a -> a.getTotalActiveUsers() != null)
            .mapToLong(CompanyEmotionalAggregate::getTotalActiveUsers)
            .sum();

        double moraleScore = calculateMoraleScore(avgStress, avgIntensity);
        double engagementScore = calculateEngagementScore(totalConversations, activeUsers);

        return new ImpactAnalysisDTO.ImpactMetrics(
            avgStress,
            avgIntensity,
            moraleScore,
            engagementScore,
            totalConversations,
            activeUsers
        );
    }

    private double calculateMoraleScore(double avgStress, double avgIntensity) {
        double stressImpact = Math.max(0, 100 - avgStress);
        double intensityImpact = Math.max(0, 100 - avgIntensity);
        return (stressImpact + intensityImpact) / 2.0;
    }

    private double calculateEngagementScore(long totalConversations, long activeUsers) {
        if (activeUsers == 0) {
            return 0.0;
        }
        double conversationsPerUser = (double) totalConversations / activeUsers;
        return Math.min(100, conversationsPerUser * 10);
    }

    private ImpactAnalysisDTO.ImpactChanges calculateChanges(
            ImpactAnalysisDTO.ImpactMetrics before,
            ImpactAnalysisDTO.ImpactMetrics after) {

        double stressChange = calculatePercentageChange(before.averageStressLevel(), after.averageStressLevel());
        double moraleChange = calculatePercentageChange(before.moraleScore(), after.moraleScore());
        double engagementChange = calculatePercentageChange(before.engagementScore(), after.engagementScore());

        String trend = determineTrend(stressChange, moraleChange, engagementChange);

        return new ImpactAnalysisDTO.ImpactChanges(
            stressChange,
            moraleChange,
            engagementChange,
            trend
        );
    }

    private double calculatePercentageChange(double before, double after) {
        if (before == 0) {
            return after > 0 ? 100.0 : 0.0;
        }
        return ((after - before) / before) * 100.0;
    }

    private String determineTrend(double stressChange, double moraleChange, double engagementChange) {
        int positive = 0;
        int negative = 0;

        if (stressChange < 0) positive++;
        else if (stressChange > 0) negative++;

        if (moraleChange > 0) positive++;
        else if (moraleChange < 0) negative++;

        if (engagementChange > 0) positive++;
        else if (engagementChange < 0) negative++;

        if (positive > negative) {
            return "POSITIVE";
        } else if (negative > positive) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    private List<ImpactAnalysisDTO.DepartmentImpact> analyzeDepartmentImpacts(
            UUID companyId,
            LocalDate beforeStart, LocalDate beforeEnd,
            LocalDate afterStart, LocalDate afterEnd) {

        List<DepartmentEmotionalAggregate> beforeDepts = departmentAggregateRepository
            .findByCompanyIdAndDateBetween(companyId, beforeStart, beforeEnd);

        List<DepartmentEmotionalAggregate> afterDepts = departmentAggregateRepository
            .findByCompanyIdAndDateBetween(companyId, afterStart, afterEnd);

        List<ImpactAnalysisDTO.DepartmentImpact> impacts = new ArrayList<>();

        var beforeByDept = beforeDepts.stream()
            .collect(Collectors.groupingBy(DepartmentEmotionalAggregate::getDepartmentId));

        var afterByDept = afterDepts.stream()
            .collect(Collectors.groupingBy(DepartmentEmotionalAggregate::getDepartmentId));

        for (UUID deptId : beforeByDept.keySet()) {
            if (afterByDept.containsKey(deptId)) {
                double beforeStress = beforeByDept.get(deptId).stream()
                    .filter(a -> a.getAvgStressLevel() != null)
                    .mapToDouble(DepartmentEmotionalAggregate::getAvgStressLevel)
                    .average()
                    .orElse(0.0);

                double afterStress = afterByDept.get(deptId).stream()
                    .filter(a -> a.getAvgStressLevel() != null)
                    .mapToDouble(DepartmentEmotionalAggregate::getAvgStressLevel)
                    .average()
                    .orElse(0.0);

                double stressChange = calculatePercentageChange(beforeStress, afterStress);
                double moraleChange = -stressChange;

                String impactLevel = Math.abs(stressChange) > 20 ? "HIGH" :
                                    Math.abs(stressChange) > 10 ? "MEDIUM" : "LOW";

                impacts.add(new ImpactAnalysisDTO.DepartmentImpact(
                    deptId,
                    null,
                    stressChange,
                    moraleChange,
                    impactLevel
                ));
            }
        }

        return impacts;
    }

    private String generateOverallAssessment(ImpactAnalysisDTO.ImpactChanges changes) {
        if (changes.trend().equals("POSITIVE")) {
            return "O evento teve impacto positivo geral, com redução de stress e melhoria na moral e engajamento.";
        } else if (changes.trend().equals("NEGATIVE")) {
            return "O evento teve impacto negativo, com aumento de stress e redução na moral e engajamento. Ação corretiva recomendada.";
        } else {
            return "O evento teve impacto neutro, com mudanças mínimas nos indicadores.";
        }
    }
}
