package br.jeanjacintho.tideflow.notification_service.dto;

import java.util.UUID;

public record UserCreatedEvent(
    UUID userId,
    String name,
    String email
) {
}




