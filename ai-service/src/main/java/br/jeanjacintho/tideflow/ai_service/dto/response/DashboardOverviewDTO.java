package br.jeanjacintho.tideflow.ai_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record DashboardOverviewDTO(
    UUID companyId,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,
    Double averageStressLevel,
    Double averageEmotionalIntensity,
    Long totalActiveUsers,
    Long totalConversations,
    Long totalMessages,
    Long riskAlertsCount,
    Map<String, Object> departmentBreakdown,
    Map<String, Integer> topKeywords,
    Map<String, Object> topTriggers
) {
}
