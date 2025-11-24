package br.jeanjacintho.tideflow.ai_service.model;

import br.jeanjacintho.tideflow.ai_service.config.MapJsonConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "department_keyword_analysis", indexes = {
    @Index(name = "idx_dept_keyword_department_date", columnList = "department_id, date"),
    @Index(name = "idx_dept_keyword_company_date", columnList = "company_id, date"),
    @Index(name = "idx_dept_keyword_date", columnList = "date DESC")
})
public class DepartmentKeywordAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "keywords", columnDefinition = "JSONB")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> keywords;

    @Column(name = "top_triggers", columnDefinition = "JSONB")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> topTriggers;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "total_messages_analyzed")
    private Long totalMessagesAnalyzed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public DepartmentKeywordAnalysis() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Map<String, Object> getKeywords() {
        return keywords;
    }

    public void setKeywords(Map<String, Object> keywords) {
        this.keywords = keywords;
    }

    public Map<String, Object> getTopTriggers() {
        return topTriggers;
    }

    public void setTopTriggers(Map<String, Object> topTriggers) {
        this.topTriggers = topTriggers;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public Long getTotalMessagesAnalyzed() {
        return totalMessagesAnalyzed;
    }

    public void setTotalMessagesAnalyzed(Long totalMessagesAnalyzed) {
        this.totalMessagesAnalyzed = totalMessagesAnalyzed;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
