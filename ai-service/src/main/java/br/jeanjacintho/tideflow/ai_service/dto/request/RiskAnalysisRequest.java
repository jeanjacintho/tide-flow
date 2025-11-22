package br.jeanjacintho.tideflow.ai_service.dto.request;

public class RiskAnalysisRequest {
    private String message;
    private String userId;
    private String conversationId;

    public RiskAnalysisRequest() {}

    public RiskAnalysisRequest(String message, String userId, String conversationId) {
        this.message = message;
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
