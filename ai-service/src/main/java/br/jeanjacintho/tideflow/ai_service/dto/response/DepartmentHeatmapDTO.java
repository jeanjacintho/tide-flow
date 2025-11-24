package br.jeanjacintho.tideflow.ai_service.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DepartmentHeatmapDTO(
    UUID companyId,
    LocalDate date,
    List<DepartmentHeatmapItem> departments
) {
    public record DepartmentHeatmapItem(
        UUID departmentId,
        String departmentName,
        Double stressLevel,
        String stressColor,
        Long activeUsers,
        Long conversations,
        Long riskAlerts,
        Map<String, Object> topKeywords,
        Map<String, Object> topTriggers
    ) {
    }
}
