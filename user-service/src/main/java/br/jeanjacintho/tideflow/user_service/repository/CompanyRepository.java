package br.jeanjacintho.tideflow.user_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {
    Optional<Company> findByDomain(String domain);
    boolean existsByDomain(String domain);
    Optional<Company> findByIdAndStatus(UUID id, CompanyStatus status);
}
