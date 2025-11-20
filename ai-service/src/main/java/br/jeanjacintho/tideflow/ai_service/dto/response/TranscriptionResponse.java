package br.jeanjacintho.tideflow.ai_service.dto.response;

public class TranscriptionResponse {
    private String transcript;
    private ConversationResponse conversationResponse;

    public TranscriptionResponse() {}

    public TranscriptionResponse(String transcript, ConversationResponse conversationResponse) {
        this.transcript = transcript;
        this.conversationResponse = conversationResponse;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public ConversationResponse getConversationResponse() {
        return conversationResponse;
    }

    public void setConversationResponse(ConversationResponse conversationResponse) {
        this.conversationResponse = conversationResponse;
    }
}

