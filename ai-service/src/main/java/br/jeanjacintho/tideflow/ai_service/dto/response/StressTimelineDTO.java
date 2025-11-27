package br.jeanjacintho.tideflow.ai_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StressTimelineDTO(
    UUID companyId,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime startDate,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime endDate,
    String granularity,
    List<StressTimelinePoint> points,
    List<StressAlert> alerts
) {
    public record StressTimelinePoint(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        Double stressLevel,
        Long activeUsers,
        Long conversations
    ) {
    }

    public record StressAlert(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String type,
        String message,
        Double stressLevel,
        Double changePercentage
    ) {
    }
}
