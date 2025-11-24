package br.jeanjacintho.tideflow.ai_service.model;

import br.jeanjacintho.tideflow.ai_service.config.MapJsonConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "company_emotional_aggregate", indexes = {
    @Index(name = "idx_company_agg_company_date", columnList = "company_id, date"),
    @Index(name = "idx_company_agg_date", columnList = "date DESC")
})
public class CompanyEmotionalAggregate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "avg_stress_level")
    private Double avgStressLevel;

    @Column(name = "department_breakdown", columnDefinition = "JSONB")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> departmentBreakdown;

    @Column(name = "total_active_users")
    private Long totalActiveUsers;

    @Column(name = "total_conversations")
    private Long totalConversations;

    @Column(name = "total_messages")
    private Long totalMessages;

    @Column(name = "risk_alerts_count")
    private Long riskAlertsCount;

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

    public CompanyEmotionalAggregate() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Map<String, Object> getDepartmentBreakdown() {
        return departmentBreakdown;
    }

    public void setDepartmentBreakdown(Map<String, Object> departmentBreakdown) {
        this.departmentBreakdown = departmentBreakdown;
    }

    public Long getTotalActiveUsers() {
        return totalActiveUsers;
    }

    public void setTotalActiveUsers(Long totalActiveUsers) {
        this.totalActiveUsers = totalActiveUsers;
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
