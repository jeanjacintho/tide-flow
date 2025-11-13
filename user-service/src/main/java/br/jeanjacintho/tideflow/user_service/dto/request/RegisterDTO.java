package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.jeanjacintho.tideflow.user_service.model.UserRole;

public record RegisterDTO(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    String email,
    
    @NotBlank(message = "Senha é obrigatória")
    String password,
    
    @NotNull(message = "Role é obrigatório")
    UserRole role
) {
    
}
