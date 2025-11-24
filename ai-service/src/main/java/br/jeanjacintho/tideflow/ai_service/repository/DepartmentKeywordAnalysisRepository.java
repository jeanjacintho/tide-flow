package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.DepartmentKeywordAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentKeywordAnalysisRepository extends JpaRepository<DepartmentKeywordAnalysis, UUID> {
    
    Optional<DepartmentKeywordAnalysis> findByDepartmentIdAndDate(UUID departmentId, LocalDate date);
    
    @Query("SELECT k FROM DepartmentKeywordAnalysis k WHERE k.departmentId = :departmentId " +
           "AND k.date >= :startDate AND k.date <= :endDate ORDER BY k.date DESC")
    List<DepartmentKeywordAnalysis> findByDepartmentIdAndDateRange(
        @Param("departmentId") UUID departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT k FROM DepartmentKeywordAnalysis k WHERE k.companyId = :companyId " +
           "AND k.date >= :startDate AND k.date <= :endDate ORDER BY k.date DESC")
    List<DepartmentKeywordAnalysis> findByCompanyIdAndDateRange(
        @Param("companyId") UUID companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query(value = "SELECT DISTINCT k.department_id FROM department_keyword_analysis k " +
           "WHERE k.department_id IS NOT NULL AND k.date = :date", nativeQuery = true)
    List<UUID> findDistinctDepartmentIdsByDate(@Param("date") LocalDate date);
}
