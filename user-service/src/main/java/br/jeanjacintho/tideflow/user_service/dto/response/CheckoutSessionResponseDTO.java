package br.jeanjacintho.tideflow.user_service.dto.response;

public record CheckoutSessionResponseDTO(
    String checkoutUrl,
    String sessionId
) {}

