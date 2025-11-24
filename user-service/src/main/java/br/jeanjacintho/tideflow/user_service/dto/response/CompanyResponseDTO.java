package br.jeanjacintho.tideflow.user_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;

public record CompanyResponseDTO(
    UUID id,
    String name,
    String domain,
    SubscriptionPlan subscriptionPlan,
    Integer maxEmployees,
    CompanyStatus status,
    String billingEmail,
    String billingAddress,
    String taxId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CompanyResponseDTO fromEntity(Company company) {
        return new CompanyResponseDTO(
            company.getId(),
            company.getName(),
            company.getDomain(),
            company.getSubscriptionPlan(),
            company.getMaxEmployees(),
            company.getStatus(),
            company.getBillingEmail(),
            company.getBillingAddress(),
            company.getTaxId(),
            company.getCreatedAt(),
            company.getUpdatedAt()
        );
    }
}
