package br.jeanjacintho.tideflow.user_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdmin;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@Service
public class CompanyAuthorizationService {

    private final CompanyAdminRepository companyAdminRepository;
    private final UserRepository userRepository;

    @Autowired
    public CompanyAuthorizationService(CompanyAdminRepository companyAdminRepository, UserRepository userRepository) {
        this.companyAdminRepository = companyAdminRepository;
        this.userRepository = userRepository;
    }

    public boolean canAccessCompany(UUID companyId) {

        if (TenantContext.isSystemAdmin()) {
            return true;
        }

        UUID currentCompanyId = TenantContext.getCompanyId();
        if (currentCompanyId == null) {
            return false;
        }
        return currentCompanyId.equals(companyId);
    }

    public boolean canManageUsers(UUID companyId) {

        if (TenantContext.isSystemAdmin()) {
            return true;
        }

        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        if (role == null) {
            return false;
        }

        CompanyAdminRole adminRole = CompanyAdminRole.valueOf(role);
        return adminRole == CompanyAdminRole.OWNER
            || adminRole == CompanyAdminRole.ADMIN
            || adminRole == CompanyAdminRole.HR_MANAGER;
    }

    public boolean canViewDashboard(UUID companyId) {

        if (TenantContext.isSystemAdmin()) {
            return true;
        }
        return canManageUsers(companyId);
    }

    public boolean isOwner(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.OWNER.name().equals(role);
    }

    public boolean isAdmin(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.ADMIN.name().equals(role);
    }

    public boolean isHrManager(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.HR_MANAGER.name().equals(role);
    }

    public Optional<CompanyAdmin> getCurrentUserAdminRole(UUID companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }

        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsernameOrEmail(username);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        return companyAdminRepository.findByUserIdAndCompanyId(user.get().getId(), companyId);
    }

    public boolean belongsToCompany(UUID companyId) {
        return canAccessCompany(companyId);
    }
}
