package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportListResponseDTO {
    
    private List<ReportSummaryDTO> reports;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;

    public ReportListResponseDTO() {}

    public ReportListResponseDTO(List<ReportSummaryDTO> reports, Long totalElements, 
                                Integer totalPages, Integer currentPage, Integer pageSize) {
        this.reports = reports;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<ReportSummaryDTO> getReports() {
        return reports;
    }

    public void setReports(List<ReportSummaryDTO> reports) {
        this.reports = reports;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public static class ReportSummaryDTO {
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
        private Boolean generatedByAi;
        private LocalDateTime createdAt;
        private LocalDateTime generatedAt;

        public ReportSummaryDTO() {}

        public ReportSummaryDTO(UUID id, UUID companyId, UUID departmentId, ReportType reportType,
                               LocalDate reportDate, LocalDate periodStart, LocalDate periodEnd,
                               ReportStatus status, String title, String executiveSummary,
                               Boolean generatedByAi, LocalDateTime createdAt, LocalDateTime generatedAt) {
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
            this.generatedByAi = generatedByAi;
            this.createdAt = createdAt;
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

        public Boolean getGeneratedByAi() {
            return generatedByAi;
        }

        public void setGeneratedByAi(Boolean generatedByAi) {
            this.generatedByAi = generatedByAi;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }
    }
}
