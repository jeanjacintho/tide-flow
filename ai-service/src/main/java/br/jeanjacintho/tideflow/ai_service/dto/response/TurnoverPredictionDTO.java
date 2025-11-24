package br.jeanjacintho.tideflow.ai_service.dto.response;

import java.util.List;
import java.util.UUID;

public record TurnoverPredictionDTO(
    UUID companyId,
    UUID departmentId,
    String departmentName,
    Integer riskScore,
    String riskLevel,
    List<TurnoverProbability> probabilities,
    List<RiskFactor> riskFactors,
    List<String> recommendations
) {
    public record TurnoverProbability(
        Integer days,
        Double probability
    ) {
    }

    public record RiskFactor(
        String factor,
        String description,
        Integer impact,
        Double weight
    ) {
    }
}
