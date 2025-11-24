package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotNull;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;
import java.util.UUID;

public record AddCompanyAdminRequestDTO(
    @NotNull(message = "ID do usuário é obrigatório")
    UUID userId,
    
    @NotNull(message = "Role é obrigatório")
    CompanyAdminRole role,
    
    String permissions
) {
}
