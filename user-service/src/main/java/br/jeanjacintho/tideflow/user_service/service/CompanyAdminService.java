package br.jeanjacintho.tideflow.user_service.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyAdminResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdmin;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@Service
public class CompanyAdminService {

    private final CompanyAdminRepository companyAdminRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyAuthorizationService authorizationService;

    @Autowired
    public CompanyAdminService(CompanyAdminRepository companyAdminRepository,
                              CompanyRepository companyRepository,
                              UserRepository userRepository,
                              CompanyAuthorizationService authorizationService) {
        this.companyAdminRepository = companyAdminRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public CompanyAdminResponseDTO addAdmin(@NonNull UUID companyId, @NonNull UUID userId, CompanyAdminRole role, String permissions) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Usuário não pertence a esta empresa");
        }

        if (!TenantContext.isSystemAdmin() && !authorizationService.isOwner(companyId) && !authorizationService.isAdmin(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Apenas OWNER ou ADMIN podem adicionar administradores");
        }

        if (companyAdminRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            throw new IllegalArgumentException("Usuário já é administrador desta empresa");
        }

        CompanyAdmin admin = new CompanyAdmin(user, company, role, permissions);
        CompanyAdmin savedAdmin = companyAdminRepository.save(admin);
        return CompanyAdminResponseDTO.fromEntity(savedAdmin);
    }

    public List<CompanyAdminResponseDTO> getCompanyAdmins(@NonNull UUID companyId) {

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        return companyAdminRepository.findByCompanyId(companyId).stream()
                .map(CompanyAdminResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAdmin(@NonNull UUID companyId, @NonNull UUID userId) {
        CompanyAdmin admin = companyAdminRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador", userId));

        if (!TenantContext.isSystemAdmin()) {
            if (!authorizationService.isOwner(companyId)) {

                String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication().getName();
                User currentUser = userRepository.findByUsernameOrEmail(username)
                        .orElseThrow(() -> new AccessDeniedException("Usuário", userId, "Usuário não encontrado"));

                if (!currentUser.getId().equals(userId)) {
                    throw new AccessDeniedException("Empresa", companyId, "Apenas OWNER pode remover administradores");
                }
            }
        }

        if (admin.getRole() == CompanyAdminRole.OWNER) {
            long ownerCount = companyAdminRepository.findByCompanyIdAndRole(companyId, CompanyAdminRole.OWNER).size();
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Não é possível remover o último OWNER da empresa");
            }
        }

        companyAdminRepository.delete(admin);
    }
}
