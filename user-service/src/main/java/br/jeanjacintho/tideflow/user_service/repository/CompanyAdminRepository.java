package br.jeanjacintho.tideflow.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.jeanjacintho.tideflow.user_service.model.CompanyAdmin;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;

@Repository
public interface CompanyAdminRepository extends JpaRepository<CompanyAdmin, UUID>, JpaSpecificationExecutor<CompanyAdmin> {
    Optional<CompanyAdmin> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    
    List<CompanyAdmin> findByCompanyId(UUID companyId);
    
    List<CompanyAdmin> findByCompanyIdAndRole(UUID companyId, CompanyAdminRole role);
    
    List<CompanyAdmin> findByUserId(UUID userId);
    
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
}
