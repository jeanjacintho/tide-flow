package br.jeanjacintho.tideflow.user_service.dto.response;

import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponseDTO(
    UUID id,
    UUID companyId,
    String planType,
    BigDecimal pricePerUser,
    Integer totalUsers,
    String billingCycle,
    LocalDate nextBillingDate,
    String status,
    BigDecimal monthlyBill
) {
    public static SubscriptionResponseDTO fromEntity(CompanySubscription subscription, BigDecimal monthlyBill) {
        return new SubscriptionResponseDTO(
            subscription.getId(),
            subscription.getCompany().getId(),
            subscription.getPlanType().name(),
            subscription.getPricePerUser(),
            subscription.getTotalUsers(),
            subscription.getBillingCycle().name(),
            subscription.getNextBillingDate(),
            subscription.getStatus().name(),
            monthlyBill
        );
    }
}
