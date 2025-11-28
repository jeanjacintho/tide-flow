package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.ReportSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportSectionRepository extends JpaRepository<ReportSection, UUID> {

    List<ReportSection> findByReportIdOrderBySectionOrderAsc(UUID reportId);

    @Query("SELECT s FROM ReportSection s WHERE s.report.id = :reportId " +
           "AND s.sectionType = :sectionType ORDER BY s.sectionOrder ASC")
    List<ReportSection> findByReportIdAndSectionType(
        @Param("reportId") UUID reportId, @Param("sectionType") String sectionType);

    void deleteByReportId(UUID reportId);
}
