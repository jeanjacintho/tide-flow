package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCompanyUserRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    String name,
    
    String username,
    
    @Email(message = "Email deve ser válido")
    @NotBlank(message = "Email é obrigatório")
    String email,
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    String password,
    
    @NotNull(message = "ID do departamento é obrigatório")
    UUID departmentId,
    
    String employeeId,
    
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String phone,
    
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    String city,
    
    @Size(max = 2, message = "Estado deve ter no máximo 2 caracteres")
    String state
) {
}
