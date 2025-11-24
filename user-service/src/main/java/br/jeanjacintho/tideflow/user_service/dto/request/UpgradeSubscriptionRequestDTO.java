package br.jeanjacintho.tideflow.user_service.dto.request;

import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;

public record UpgradeSubscriptionRequestDTO(
    SubscriptionPlan planType
) {
}
