package br.jeanjacintho.tideflow.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jeanjacintho.tideflow.user_service.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    Optional<User> findByAnonymizedId(UUID anonymizedId);
    
    List<User> findByCompanyId(UUID companyId);
    List<User> findByCompanyIdAndIsActive(UUID companyId, Boolean isActive);
    
    List<User> findByDepartmentId(UUID departmentId);
    List<User> findByDepartmentIdAndIsActive(UUID departmentId, Boolean isActive);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId AND u.isActive = true")
    Long countActiveUsersByCompanyId(@Param("companyId") UUID companyId);
    
    boolean existsByCompanyIdAndEmployeeId(UUID companyId, String employeeId);
}
