package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emotional_analysis")
public class EmotionalAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
}
