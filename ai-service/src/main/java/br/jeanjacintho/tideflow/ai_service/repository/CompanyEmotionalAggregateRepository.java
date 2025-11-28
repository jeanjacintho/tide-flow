package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.CompanyEmotionalAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyEmotionalAggregateRepository extends JpaRepository<CompanyEmotionalAggregate, UUID> {

    Optional<CompanyEmotionalAggregate> findByCompanyIdAndDate(UUID companyId, LocalDate date);

    List<CompanyEmotionalAggregate> findByCompanyIdAndDateBetween(
        UUID companyId,
        LocalDate startDate,
        LocalDate endDate
    );
}
