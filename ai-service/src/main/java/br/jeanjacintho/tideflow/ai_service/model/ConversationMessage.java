package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_messages", indexes = {
        @Index(name = "idx_message_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_message_sequence", columnList = "conversation_id, sequence_number"),
        @Index(name = "idx_message_created_at", columnList = "created_at"),
        @Index(name = "idx_message_role_created", columnList = "role, created_at")
})
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @PrePersist
    protected void onCreate() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ConversationMessage(UUID id, Conversation conversation, MessageRole role, String content, LocalDateTime createdAt, Integer sequenceNumber) {
        this.id = id;
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.sequenceNumber = sequenceNumber;
    }

    public ConversationMessage(MessageRole role, String content, Integer sequenceNumber) {
        this.role = role;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }

    public ConversationMessage() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
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
