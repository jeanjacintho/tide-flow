package br.jeanjacintho.tideflow.notification_service.dto;

import java.util.UUID;

public record RiskAlertEvent(
    UUID userId,
    String userName,
    String userEmail,
    String trustedEmail,
    String message,
    String riskLevel,
    String reason,
    String context
) {
}
