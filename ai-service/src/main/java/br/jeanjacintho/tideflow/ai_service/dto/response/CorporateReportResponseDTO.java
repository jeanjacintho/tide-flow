package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CorporateReportResponseDTO {
    
    private UUID id;
    private UUID companyId;
    private UUID departmentId;
    private ReportType reportType;
    private LocalDate reportDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private ReportStatus status;
    private String title;
    private String executiveSummary;
    private Map<String, Object> insights;
    private Map<String, Object> metrics;
    private String recommendations;
    private Boolean generatedByAi;
    private String aiModelVersion;
    private Long generationTimeMs;
    private List<ReportSectionDTO> sections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime generatedAt;

    public CorporateReportResponseDTO() {}

    public CorporateReportResponseDTO(UUID id, UUID companyId, UUID departmentId, ReportType reportType,
                                     LocalDate reportDate, LocalDate periodStart, LocalDate periodEnd,
                                     ReportStatus status, String title, String executiveSummary,
                                     Map<String, Object> insights, Map<String, Object> metrics,
                                     String recommendations, Boolean generatedByAi, String aiModelVersion,
                                     Long generationTimeMs, List<ReportSectionDTO> sections,
                                     LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime generatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.departmentId = departmentId;
        this.reportType = reportType;
        this.reportDate = reportDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.title = title;
        this.executiveSummary = executiveSummary;
        this.insights = insights;
        this.metrics = metrics;
        this.recommendations = recommendations;
        this.generatedByAi = generatedByAi;
        this.aiModelVersion = aiModelVersion;
        this.generationTimeMs = generationTimeMs;
        this.sections = sections;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.generatedAt = generatedAt;
    }

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

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public Map<String, Object> getInsights() {
        return insights;
    }

    public void setInsights(Map<String, Object> insights) {
        this.insights = insights;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public Boolean getGeneratedByAi() {
        return generatedByAi;
    }

    public void setGeneratedByAi(Boolean generatedByAi) {
        this.generatedByAi = generatedByAi;
    }

    public String getAiModelVersion() {
        return aiModelVersion;
    }

    public void setAiModelVersion(String aiModelVersion) {
        this.aiModelVersion = aiModelVersion;
    }

    public Long getGenerationTimeMs() {
        return generationTimeMs;
    }

    public void setGenerationTimeMs(Long generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }

    public List<ReportSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<ReportSectionDTO> sections) {
        this.sections = sections;
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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public static class ReportSectionDTO {
        private UUID id;
        private String sectionType;
        private Integer sectionOrder;
        private String title;
        private String content;
        private Map<String, Object> data;
        private Map<String, Object> visualizationConfig;

        public ReportSectionDTO() {}

        public ReportSectionDTO(UUID id, String sectionType, Integer sectionOrder, String title,
                               String content, Map<String, Object> data, Map<String, Object> visualizationConfig) {
            this.id = id;
            this.sectionType = sectionType;
            this.sectionOrder = sectionOrder;
            this.title = title;
            this.content = content;
            this.data = data;
            this.visualizationConfig = visualizationConfig;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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
    }
}
