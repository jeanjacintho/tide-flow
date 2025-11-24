package br.jeanjacintho.tideflow.ai_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StressTimelineDTO(
    UUID companyId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String granularity,
    List<StressTimelinePoint> points,
    List<StressAlert> alerts
) {
    public record StressTimelinePoint(
        LocalDateTime timestamp,
        Double stressLevel,
        Long activeUsers,
        Long conversations
    ) {
    }

    public record StressAlert(
        LocalDateTime timestamp,
        String type,
        String message,
        Double stressLevel,
        Double changePercentage
    ) {
    }
}
