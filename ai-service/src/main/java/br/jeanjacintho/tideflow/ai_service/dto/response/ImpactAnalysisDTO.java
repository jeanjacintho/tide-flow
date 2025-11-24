package br.jeanjacintho.tideflow.ai_service.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ImpactAnalysisDTO(
    UUID companyId,
    LocalDate eventDate,
    String eventDescription,
    ImpactMetrics beforeMetrics,
    ImpactMetrics afterMetrics,
    ImpactChanges changes,
    List<DepartmentImpact> departmentImpacts,
    String overallAssessment
) {
    public record ImpactMetrics(
        Double averageStressLevel,
        Double averageEmotionalIntensity,
        Double moraleScore,
        Double engagementScore,
        Long totalConversations,
        Long activeUsers
    ) {
    }

    public record ImpactChanges(
        Double stressChangePercentage,
        Double moraleChangePercentage,
        Double engagementChangePercentage,
        String trend
    ) {
    }

    public record DepartmentImpact(
        UUID departmentId,
        String departmentName,
        Double stressChangePercentage,
        Double moraleChangePercentage,
        String impactLevel
    ) {
    }
}
