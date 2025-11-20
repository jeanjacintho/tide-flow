package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emotional_analysis", indexes = {
    @Index(name = "idx_emotional_user_message", columnList = "usuario_id, message_id"),
    @Index(name = "idx_emotional_conversation", columnList = "conversation_id, sequence_number"),
    @Index(name = "idx_emotional_primary", columnList = "primary_emotional"),
    @Index(name = "idx_emotional_intensity", columnList = "intensity DESC"),
    @Index(name = "idx_emotional_user_primary", columnList = "usuario_id, primary_emotional")
})
public class EmotionalAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "primary_emotional", nullable = false, length = 50)
    private String primaryEmotional;

    @Column(nullable = false)
    private Integer intensity;

    @ElementCollection
    @CollectionTable(name = "emotional_triggers", joinColumns = @JoinColumn(name = "emotional_analysis_id"))
    @Column(name = "trigger")
    private List<String> triggers;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    public EmotionalAnalysis() {}

    public EmotionalAnalysis(String primaryEmotional, Integer intensity, List<String> triggers, String context, String suggestion) {
        this.primaryEmotional = primaryEmotional;
        this.intensity = intensity;
        this.triggers = triggers;
        this.context = context;
        this.suggestion = suggestion;
    }

    public EmotionalAnalysis(String usuarioId, UUID conversationId, UUID messageId, Integer sequenceNumber,
                            String primaryEmotional, Integer intensity, List<String> triggers, 
                            String context, String suggestion) {
        this.usuarioId = usuarioId;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.sequenceNumber = sequenceNumber;
        this.primaryEmotional = primaryEmotional;
        this.intensity = intensity;
        this.triggers = triggers;
        this.context = context;
        this.suggestion = suggestion;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPrimaryEmotional() {
        return primaryEmotional;
    }

    public void setPrimaryEmotional(String primaryEmotional) {
        this.primaryEmotional = primaryEmotional;
    }

    public Integer getIntensity() {
        return intensity;
    }

    public void setIntensity(Integer intensity) {
        this.intensity = intensity;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
