package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
    @NotBlank(message = "Email ou username é obrigatório")
    String username,

    @NotBlank(message = "Senha é obrigatória")
    String password
) {

}
