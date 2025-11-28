package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDTO(
    @NotBlank(message = "Nome é obrigatório")
    String name,

    String username,

    @Email(message = "Email deve ser válido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password
) {

}
