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

/**
 * Serviço responsável por validar permissões de acesso baseadas em CompanyAdmin.
 * Garante que usuários só acessem dados de sua própria empresa.
 */
@Service
public class CompanyAuthorizationService {

    private final CompanyAdminRepository companyAdminRepository;
    private final UserRepository userRepository;

    @Autowired
    public CompanyAuthorizationService(CompanyAdminRepository companyAdminRepository, UserRepository userRepository) {
        this.companyAdminRepository = companyAdminRepository;
        this.userRepository = userRepository;
    }

    /**
     * Verifica se o usuário autenticado pode acessar a empresa especificada.
     * SYSTEM_ADMIN tem acesso a todas as empresas.
     */
    public boolean canAccessCompany(UUID companyId) {
        // SYSTEM_ADMIN tem acesso total
        if (TenantContext.isSystemAdmin()) {
            return true;
        }
        
        UUID currentCompanyId = TenantContext.getCompanyId();
        if (currentCompanyId == null) {
            return false;
        }
        return currentCompanyId.equals(companyId);
    }

    /**
     * Verifica se o usuário autenticado tem permissão para gerenciar usuários da empresa.
     * OWNER, ADMIN e HR_MANAGER podem gerenciar usuários.
     * SYSTEM_ADMIN tem acesso total.
     */
    public boolean canManageUsers(UUID companyId) {
        // SYSTEM_ADMIN tem acesso total
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

    /**
     * Verifica se o usuário autenticado pode visualizar o dashboard corporativo.
     * OWNER, ADMIN e HR_MANAGER podem visualizar o dashboard.
     * SYSTEM_ADMIN tem acesso total.
     */
    public boolean canViewDashboard(UUID companyId) {
        // SYSTEM_ADMIN tem acesso total
        if (TenantContext.isSystemAdmin()) {
            return true;
        }
        return canManageUsers(companyId);
    }

    /**
     * Verifica se o usuário autenticado é OWNER da empresa.
     */
    public boolean isOwner(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.OWNER.name().equals(role);
    }

    /**
     * Verifica se o usuário autenticado é ADMIN da empresa.
     */
    public boolean isAdmin(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.ADMIN.name().equals(role);
    }

    /**
     * Verifica se o usuário autenticado é HR_MANAGER da empresa.
     */
    public boolean isHrManager(UUID companyId) {
        if (!canAccessCompany(companyId)) {
            return false;
        }

        String role = TenantContext.getCompanyRole();
        return CompanyAdminRole.HR_MANAGER.name().equals(role);
    }

    /**
     * Obtém o CompanyAdmin do usuário autenticado para a empresa especificada.
     */
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

    /**
     * Valida se o usuário autenticado pertence à empresa especificada.
     */
    public boolean belongsToCompany(UUID companyId) {
        return canAccessCompany(companyId);
    }
}
