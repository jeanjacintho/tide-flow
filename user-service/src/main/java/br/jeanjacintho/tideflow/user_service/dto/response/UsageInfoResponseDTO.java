package br.jeanjacintho.tideflow.user_service.dto.response;

import br.jeanjacintho.tideflow.user_service.service.UsageTrackingService;

public record UsageInfoResponseDTO(
    Integer activeUsers,
    Integer maxUsers,
    Boolean atLimit,
    Integer remainingSlots
) {
    public static UsageInfoResponseDTO fromUsageInfo(UsageTrackingService.UsageInfo usageInfo) {
        return new UsageInfoResponseDTO(
            usageInfo.getActiveUsers(),
            usageInfo.getMaxUsers(),
            usageInfo.isAtLimit(),
            usageInfo.getRemainingSlots()
        );
    }
}
