package br.jeanjacintho.tideflow.user_service.dto.response;

import br.jeanjacintho.tideflow.user_service.model.PaymentHistory;
import br.jeanjacintho.tideflow.user_service.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistoryResponseDTO(
    UUID id,
    UUID companyId,
    UUID subscriptionId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    LocalDateTime paymentDate,
    LocalDateTime billingPeriodStart,
    LocalDateTime billingPeriodEnd,
    String stripeInvoiceId,
    String stripePaymentIntentId,
    String stripeChargeId,
    String stripeCustomerId,
    String stripeSubscriptionId,
    String invoiceNumber,
    String description,
    LocalDateTime createdAt
) {
    public static PaymentHistoryResponseDTO fromEntity(PaymentHistory payment) {
        return new PaymentHistoryResponseDTO(
            payment.getId(),
            payment.getCompany().getId(),
            payment.getSubscription().getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getPaymentDate(),
            payment.getBillingPeriodStart(),
            payment.getBillingPeriodEnd(),
            payment.getStripeInvoiceId(),
            payment.getStripePaymentIntentId(),
            payment.getStripeChargeId(),
            payment.getStripeCustomerId(),
            payment.getStripeSubscriptionId(),
            payment.getInvoiceNumber(),
            payment.getDescription(),
            payment.getCreatedAt()
        );
    }
}
