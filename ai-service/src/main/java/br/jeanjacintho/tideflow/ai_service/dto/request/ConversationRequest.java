package br.jeanjacintho.tideflow.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConversationRequest {

    @NotBlank(message = "userId é obrigatório")
    private String userId;

    @NotBlank(message = "message é obrigatória")
    private String message;

    private String conversationId;

    public ConversationRequest() {}

    public ConversationRequest(String userId, String message, String conversationId) {
        this.userId = userId;
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
