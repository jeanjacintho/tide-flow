package br.jeanjacintho.tideflow.user_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import br.jeanjacintho.tideflow.user_service.model.CompanyAdmin;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;

public record CompanyAdminResponseDTO(
    UUID id,
    UUID userId,
    String userName,
    String userEmail,
    UUID companyId,
    String companyName,
    CompanyAdminRole role,
    String permissions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CompanyAdminResponseDTO fromEntity(CompanyAdmin admin) {
        return new CompanyAdminResponseDTO(
            admin.getId(),
            admin.getUser().getId(),
            admin.getUser().getName(),
            admin.getUser().getEmail(),
            admin.getCompany().getId(),
            admin.getCompany().getName(),
            admin.getRole(),
            admin.getPermissions(),
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}
