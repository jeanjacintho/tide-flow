package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;

public class ConversationResponse {
    private String aiResponse;
    private String conversationId;
    private Boolean isComplete;
    private EmotionalAnalysis analisys;

    public ConversationResponse(String aiResponse, String conversationId, Boolean isComplete, EmotionalAnalysis analisys) {
        this.aiResponse = aiResponse;
        this.conversationId = conversationId;
        this.isComplete = isComplete;
        this.analisys = analisys;
    }

    public ConversationResponse() {}

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Boolean getComplete() {
        return isComplete;
    }

    public void setComplete(Boolean complete) {
        isComplete = complete;
    }

    public EmotionalAnalysis getAnalisys() {
        return analisys;
    }

    public void setAnalisys(EmotionalAnalysis analisys) {
        this.analisys = analisys;
    }
}
