package br.jeanjacintho.tideflow.ai_service.dto.response;

public class RiskAnalysisResponse {
    private boolean isRiskDetected;
    private String riskLevel;
    private String reason;
    private String context;
    private double confidence;

    public RiskAnalysisResponse() {}

    public RiskAnalysisResponse(boolean isRiskDetected, String riskLevel, String reason, String context, double confidence) {
        this.isRiskDetected = isRiskDetected;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.context = context;
        this.confidence = confidence;
    }

    public boolean isRiskDetected() {
        return isRiskDetected;
    }

    public void setRiskDetected(boolean riskDetected) {
        isRiskDetected = riskDetected;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
