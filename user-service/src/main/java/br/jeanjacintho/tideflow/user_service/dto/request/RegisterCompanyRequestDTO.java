package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCompanyRequestDTO(
    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String companyName,
    
    @Size(max = 255, message = "Domínio deve ter no máximo 255 caracteres")
    String companyDomain,
    
    @NotBlank(message = "Nome do proprietário é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String ownerName,
    
    @NotBlank(message = "Email do proprietário é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    String ownerEmail,
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    String password
) {
}
