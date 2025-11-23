package br.jeanjacintho.tideflow.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.jeanjacintho.tideflow.user_service.model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID>, JpaSpecificationExecutor<Department> {
    List<Department> findByCompanyId(UUID companyId);
    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
