package br.jeanjacintho.tideflow.user_service.dto.response;

import br.jeanjacintho.tideflow.user_service.service.BillingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponseDTO(
    UUID companyId,
    String companyName,
    String period,
    LocalDate startDate,
    LocalDate endDate,
    String planType,
    Integer activeUsers,
    BigDecimal pricePerUser,
    BigDecimal totalAmount,
    String billingCycle,
    String status
) {
    public static InvoiceResponseDTO fromInvoiceDTO(BillingService.InvoiceDTO invoice) {
        return new InvoiceResponseDTO(
            invoice.getCompanyId(),
            invoice.getCompanyName(),
            invoice.getPeriod(),
            invoice.getStartDate(),
            invoice.getEndDate(),
            invoice.getPlanType(),
            invoice.getActiveUsers(),
            invoice.getPricePerUser(),
            invoice.getTotalAmount(),
            invoice.getBillingCycle(),
            invoice.getStatus()
        );
    }
}
