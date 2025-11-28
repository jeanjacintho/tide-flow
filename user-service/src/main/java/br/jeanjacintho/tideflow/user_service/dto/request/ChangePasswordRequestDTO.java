package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
    @NotBlank(message = "Senha atual é obrigatória")
    String currentPassword,

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter pelo menos 8 caracteres")
    String newPassword
) {
}
