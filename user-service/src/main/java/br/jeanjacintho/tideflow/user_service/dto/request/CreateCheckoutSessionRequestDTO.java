package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCheckoutSessionRequestDTO(
    @NotNull UUID companyId,
    String successUrl,
    String cancelUrl
) {}
