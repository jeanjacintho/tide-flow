package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.CorporateReport;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CorporateReportRepository extends JpaRepository<CorporateReport, UUID> {
    
    Page<CorporateReport> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
    
    Page<CorporateReport> findByCompanyIdAndReportTypeOrderByCreatedAtDesc(
        UUID companyId, ReportType reportType, Pageable pageable);
    
    Page<CorporateReport> findByCompanyIdAndStatusOrderByCreatedAtDesc(
        UUID companyId, ReportStatus status, Pageable pageable);
    
    List<CorporateReport> findByCompanyIdAndReportDateBetweenOrderByReportDateDesc(
        UUID companyId, LocalDate startDate, LocalDate endDate);
    
    Optional<CorporateReport> findByCompanyIdAndReportTypeAndReportDate(
        UUID companyId, ReportType reportType, LocalDate reportDate);
    
    @Query("SELECT r FROM CorporateReport r WHERE r.companyId = :companyId " +
           "AND r.periodStart <= :date AND r.periodEnd >= :date " +
           "ORDER BY r.createdAt DESC")
    List<CorporateReport> findActiveReportsForDate(
        @Param("companyId") UUID companyId, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(r) FROM CorporateReport r WHERE r.companyId = :companyId " +
           "AND r.status = :status")
    Long countByCompanyIdAndStatus(
        @Param("companyId") UUID companyId, @Param("status") ReportStatus status);
    
    @Query("SELECT r FROM CorporateReport r WHERE r.companyId = :companyId " +
           "AND r.departmentId = :departmentId " +
           "AND r.reportDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.reportDate DESC")
    List<CorporateReport> findByCompanyAndDepartmentAndDateRange(
        @Param("companyId") UUID companyId,
        @Param("departmentId") UUID departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    List<CorporateReport> findByStatusAndGeneratedAtBefore(
        ReportStatus status, java.time.LocalDateTime before);
    
    Page<CorporateReport> findByCompanyIdAndReportTypeAndStatusOrderByCreatedAtDesc(
        UUID companyId, ReportType reportType, ReportStatus status, Pageable pageable);
}
