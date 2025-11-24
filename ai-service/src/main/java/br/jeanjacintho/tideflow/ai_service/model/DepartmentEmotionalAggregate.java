package br.jeanjacintho.tideflow.ai_service.model;

import br.jeanjacintho.tideflow.ai_service.config.MapStringIntegerConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "department_emotional_aggregate", indexes = {
    @Index(name = "idx_dept_agg_department_date", columnList = "department_id, date"),
    @Index(name = "idx_dept_agg_company_date", columnList = "company_id, date"),
    @Index(name = "idx_dept_agg_date", columnList = "date DESC")
})
public class DepartmentEmotionalAggregate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "avg_stress_level")
    private Double avgStressLevel;

    @Column(name = "avg_emotional_intensity")
    private Double avgEmotionalIntensity;

    @Column(name = "primary_emotions", columnDefinition = "JSONB")
    @Convert(converter = MapStringIntegerConverter.class)
    private Map<String, Integer> primaryEmotions;

    @Column(name = "total_conversations")
    private Long totalConversations;

    @Column(name = "total_messages")
    private Long totalMessages;

    @Column(name = "risk_alerts_count")
    private Long riskAlertsCount;

    @Column(name = "unique_users_count")
    private Long uniqueUsersCount;

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

    public DepartmentEmotionalAggregate() {}

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

    public Double getAvgStressLevel() {
        return avgStressLevel;
    }

    public void setAvgStressLevel(Double avgStressLevel) {
        this.avgStressLevel = avgStressLevel;
    }

    public Double getAvgEmotionalIntensity() {
        return avgEmotionalIntensity;
    }

    public void setAvgEmotionalIntensity(Double avgEmotionalIntensity) {
        this.avgEmotionalIntensity = avgEmotionalIntensity;
    }

    public Map<String, Integer> getPrimaryEmotions() {
        return primaryEmotions;
    }

    public void setPrimaryEmotions(Map<String, Integer> primaryEmotions) {
        this.primaryEmotions = primaryEmotions;
    }

    public Long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(Long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public Long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Long getRiskAlertsCount() {
        return riskAlertsCount;
    }

    public void setRiskAlertsCount(Long riskAlertsCount) {
        this.riskAlertsCount = riskAlertsCount;
    }

    public Long getUniqueUsersCount() {
        return uniqueUsersCount;
    }

    public void setUniqueUsersCount(Long uniqueUsersCount) {
        this.uniqueUsersCount = uniqueUsersCount;
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
