package br.jeanjacintho.tideflow.user_service.event;

import java.util.UUID;

public record UserCreatedEvent(
    UUID userId,
    String name,
    String email
) {
}




