package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentEmotionalAggregateRepository extends JpaRepository<DepartmentEmotionalAggregate, UUID> {
    
    Optional<DepartmentEmotionalAggregate> findByDepartmentIdAndDate(UUID departmentId, LocalDate date);
    
    List<DepartmentEmotionalAggregate> findByDepartmentIdAndDateBetween(
        UUID departmentId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    List<DepartmentEmotionalAggregate> findByCompanyIdAndDateBetween(
        UUID companyId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    @Query("SELECT d FROM DepartmentEmotionalAggregate d WHERE d.companyId = :companyId AND d.date = :date")
    List<DepartmentEmotionalAggregate> findAllByCompanyIdAndDate(
        @Param("companyId") UUID companyId,
        @Param("date") LocalDate date
    );
}
