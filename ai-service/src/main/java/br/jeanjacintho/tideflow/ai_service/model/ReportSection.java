package br.jeanjacintho.tideflow.ai_service.model;

import br.jeanjacintho.tideflow.ai_service.config.MapJsonConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "report_sections", indexes = {
    @Index(name = "idx_section_report", columnList = "report_id, section_order"),
    @Index(name = "idx_section_type", columnList = "section_type")
})
public class ReportSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private CorporateReport report;

    @Column(name = "section_type", nullable = false, length = 50)
    private String sectionType;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "data", columnDefinition = "JSONB")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> data;

    @Column(name = "visualization_config", columnDefinition = "JSONB")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> visualizationConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ReportSection() {}

    public ReportSection(CorporateReport report, String sectionType, Integer sectionOrder, String title) {
        this.report = report;
        this.sectionType = sectionType;
        this.sectionOrder = sectionOrder;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CorporateReport getReport() {
        return report;
    }

    public void setReport(CorporateReport report) {
        this.report = report;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getSectionOrder() {
        return sectionOrder;
    }

    public void setSectionOrder(Integer sectionOrder) {
        this.sectionOrder = sectionOrder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getVisualizationConfig() {
        return visualizationConfig;
    }

    public void setVisualizationConfig(Map<String, Object> visualizationConfig) {
        this.visualizationConfig = visualizationConfig;
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
}
