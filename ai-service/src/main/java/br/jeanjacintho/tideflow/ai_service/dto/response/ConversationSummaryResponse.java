package br.jeanjacintho.tideflow.ai_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class ConversationSummaryResponse {
    private UUID conversationId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long messageCount;
    private String lastMessagePreview;

    public ConversationSummaryResponse() {}

    public ConversationSummaryResponse(UUID conversationId, String userId,
                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                      Long messageCount, String lastMessagePreview) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messageCount = messageCount;
        this.lastMessagePreview = lastMessagePreview;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }
}
