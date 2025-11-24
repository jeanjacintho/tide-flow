package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequestDTO(
    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String name,
    
    @Size(max = 255, message = "Domínio deve ter no máximo 255 caracteres")
    String domain,
    
    String billingEmail,
    
    String billingAddress,
    
    @Size(max = 50, message = "CNPJ deve ter no máximo 50 caracteres")
    String taxId
) {
}
