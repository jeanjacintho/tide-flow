package br.jeanjacintho.tideflow.user_service.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.dto.request.CompanyRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.RegisterCompanyRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdmin;
import br.jeanjacintho.tideflow.user_service.model.CompanyAdminRole;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.Department;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.model.SystemRole;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.repository.DepartmentRepository;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;
    private final SubscriptionService subscriptionService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CompanyAdminRepository companyAdminRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final BigDecimal FREE_PLAN_PRICE = BigDecimal.ZERO;

    @Autowired
    public CompanyService(
            CompanyRepository companyRepository,
            CompanyAuthorizationService authorizationService,
            SubscriptionService subscriptionService,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            CompanyAdminRepository companyAdminRepository,
            CompanySubscriptionRepository subscriptionRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
        this.subscriptionService = subscriptionService;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.companyAdminRepository = companyAdminRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CompanyResponseDTO createCompany(CompanyRequestDTO requestDTO) {

        if (!TenantContext.isSystemAdmin()) {
            throw new AccessDeniedException("Empresa", null, "Apenas SYSTEM_ADMIN pode criar empresas");
        }

        if (requestDTO.domain() != null && !requestDTO.domain().isEmpty()) {
            if (companyRepository.existsByDomain(requestDTO.domain())) {
                throw new IllegalArgumentException("Domínio já está em uso: " + requestDTO.domain());
            }
        }

        Company company = new Company();
        company.setName(requestDTO.name());
        company.setDomain(requestDTO.domain());
        company.setBillingEmail(requestDTO.billingEmail());
        company.setBillingAddress(requestDTO.billingAddress());
        company.setTaxId(requestDTO.taxId());
        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setMaxEmployees(7);
        company.setStatus(CompanyStatus.TRIAL);

        Company savedCompany = companyRepository.save(company);

        UUID companyId = savedCompany.getId();
        if (companyId != null) {
            final UUID finalCompanyId = companyId;
            try {
                subscriptionService.createSubscription(finalCompanyId, SubscriptionPlan.FREE);
            } catch (Exception e) {

                org.slf4j.LoggerFactory.getLogger(CompanyService.class)
                    .warn("Erro ao criar assinatura para empresa {}: {}", finalCompanyId, e.getMessage());
            }
        }

        return CompanyResponseDTO.fromEntity(savedCompany);
    }

    public CompanyResponseDTO getCompanyById(@NonNull UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));

        if (!authorizationService.canAccessCompany(id)) {
            throw new AccessDeniedException("Empresa", id, "Usuário não tem acesso a esta empresa");
        }

        return CompanyResponseDTO.fromEntity(company);
    }

    @Transactional
    public CompanyResponseDTO updateCompany(@NonNull UUID id, CompanyRequestDTO requestDTO) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));

        if (!TenantContext.isSystemAdmin() && !authorizationService.isOwner(id) && !authorizationService.isAdmin(id)) {
            throw new AccessDeniedException("Empresa", id, "Apenas OWNER ou ADMIN podem atualizar a empresa");
        }

        if (requestDTO.domain() != null && !requestDTO.domain().isEmpty()
            && !requestDTO.domain().equals(company.getDomain())) {
            if (companyRepository.existsByDomain(requestDTO.domain())) {
                throw new IllegalArgumentException("Domínio já está em uso: " + requestDTO.domain());
            }
        }

        company.setName(requestDTO.name());
        company.setDomain(requestDTO.domain());
        company.setBillingEmail(requestDTO.billingEmail());
        company.setBillingAddress(requestDTO.billingAddress());
        company.setTaxId(requestDTO.taxId());

        Company updatedCompany = companyRepository.save(company);
        return CompanyResponseDTO.fromEntity(updatedCompany);
    }

    public List<CompanyResponseDTO> getAllCompanies() {

        if (TenantContext.isSystemAdmin()) {
            return companyRepository.findAll().stream()
                    .map(CompanyResponseDTO::fromEntity)
                    .collect(Collectors.toList());
        } else {
            UUID currentCompanyId = TenantContext.getCompanyId();
            if (currentCompanyId == null) {
                return List.of();
            }
            return companyRepository.findById(currentCompanyId)
                    .map(company -> List.of(CompanyResponseDTO.fromEntity(company)))
                    .orElse(List.of());
        }
    }

    @Transactional
    public CompanyResponseDTO registerCompanyPublic(RegisterCompanyRequestDTO requestDTO) {

        if (userRepository.existsByEmail(requestDTO.ownerEmail())) {
            throw new IllegalArgumentException("Email já está em uso: " + requestDTO.ownerEmail());
        }

        if (requestDTO.companyDomain() != null && !requestDTO.companyDomain().isEmpty()) {
            if (companyRepository.existsByDomain(requestDTO.companyDomain())) {
                throw new IllegalArgumentException("Domínio já está em uso: " + requestDTO.companyDomain());
            }
        }

        Company company = new Company();
        company.setName(requestDTO.companyName());
        company.setDomain(requestDTO.companyDomain());
        company.setBillingEmail(requestDTO.ownerEmail());
        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setMaxEmployees(7);
        company.setStatus(CompanyStatus.TRIAL);

        Company savedCompany = companyRepository.save(company);
        UUID companyId = savedCompany.getId();

        if (companyId == null) {
            throw new IllegalStateException("Erro ao criar empresa: ID não gerado");
        }

        CompanySubscription subscription = new CompanySubscription(
            savedCompany,
            SubscriptionPlan.FREE,
            FREE_PLAN_PRICE,
            0,
            BillingCycle.MONTHLY,
            LocalDateTime.now().plusMonths(1)
        );
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscriptionRepository.save(subscription);

        Department defaultDepartment = new Department(savedCompany, "Geral", "Departamento padrão");
        Department savedDepartment = departmentRepository.save(defaultDepartment);

        String baseUsername = requestDTO.ownerEmail().split("@")[0];
        String username = baseUsername;
        int attempts = 0;
        while (userRepository.existsByUsername(username) && attempts < 10) {
            username = baseUsername + "_" + attempts;
            attempts++;
        }
        if (userRepository.existsByUsername(username)) {

            username = baseUsername + "_" + System.currentTimeMillis();
        }

        User owner = new User();
        owner.setName(requestDTO.ownerName());
        owner.setEmail(requestDTO.ownerEmail());
        owner.setUsername(username);
        owner.setPassword(passwordEncoder.encode(requestDTO.password()));
        owner.setCompany(savedCompany);
        owner.setDepartment(savedDepartment);
        owner.setSystemRole(SystemRole.NORMAL);
        owner.setIsActive(true);
        owner.setMustChangePassword(false);

        User savedOwner = userRepository.save(owner);

        CompanyAdmin companyAdmin = new CompanyAdmin(savedOwner, savedCompany, CompanyAdminRole.OWNER, null);
        CompanyAdmin savedCompanyAdmin = companyAdminRepository.save(companyAdmin);

        if (savedCompanyAdmin == null || savedCompanyAdmin.getRole() != CompanyAdminRole.OWNER) {
            throw new IllegalStateException("Erro ao criar CompanyAdmin com role OWNER para o primeiro usuário");
        }

        org.slf4j.LoggerFactory.getLogger(CompanyService.class)
            .info("CompanyAdmin criado com sucesso: userId={}, companyId={}, role={}",
                savedOwner.getId(), savedCompany.getId(), savedCompanyAdmin.getRole());

        return CompanyResponseDTO.fromEntity(savedCompany);
    }
}
