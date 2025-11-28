package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConversationHistoryResponse {
    private UUID conversationId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;

    public ConversationHistoryResponse() {}

    public ConversationHistoryResponse(UUID conversationId, String userId,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       List<ConversationMessage> messages) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = messages.stream()
                .map(msg -> new MessageResponse(
                        msg.getId(),
                        msg.getRole(),
                        msg.getContent(),
                        msg.getCreatedAt(),
                        msg.getSequenceNumber()
                ))
                .collect(Collectors.toList());
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

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }

    public static class MessageResponse {
        private UUID id;
        private MessageRole role;
        private String content;
        private LocalDateTime createdAt;
        private Integer sequenceNumber;

        public MessageResponse() {}

        public MessageResponse(UUID id, MessageRole role, String content,
                              LocalDateTime createdAt, Integer sequenceNumber) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
            this.sequenceNumber = sequenceNumber;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public MessageRole getRole() {
            return role;
        }

        public void setRole(MessageRole role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public Integer getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }
    }
}
