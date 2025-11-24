package br.jeanjacintho.tideflow.user_service.dto.response;

import java.util.UUID;

public record InviteUserResponseDTO(
    UUID userId,
    String username,
    String temporaryPassword,
    String message
) {
}
