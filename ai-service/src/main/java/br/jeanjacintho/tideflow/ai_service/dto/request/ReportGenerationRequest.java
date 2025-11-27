package br.jeanjacintho.tideflow.ai_service.dto.request;

import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ReportGenerationRequest {
    
    @NotNull(message = "Company ID is required")
    private UUID companyId;
    
    private UUID departmentId;
    
    @NotNull(message = "Report type is required")
    private ReportType reportType;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodStart;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodEnd;
    
    private String title;
    
    private List<String> includeSections;
    
    private Boolean generateInsights = true;
    
    private Boolean generateRecommendations = true;
    
    private String customPrompt;
    
    private String eventDescription;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;

    public ReportGenerationRequest() {}

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getIncludeSections() {
        return includeSections;
    }

    public void setIncludeSections(List<String> includeSections) {
        this.includeSections = includeSections;
    }

    public Boolean getGenerateInsights() {
        return generateInsights;
    }

    public void setGenerateInsights(Boolean generateInsights) {
        this.generateInsights = generateInsights;
    }

    public Boolean getGenerateRecommendations() {
        return generateRecommendations;
    }

    public void setGenerateRecommendations(Boolean generateRecommendations) {
        this.generateRecommendations = generateRecommendations;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }
}
