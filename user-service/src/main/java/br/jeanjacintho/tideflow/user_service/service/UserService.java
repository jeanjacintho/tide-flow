package br.jeanjacintho.tideflow.user_service.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.dto.request.ChangePasswordRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.CreateCompanyUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.InviteUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.RegisterDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpdateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.InviteUserResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.DuplicateEmailException;
import br.jeanjacintho.tideflow.user_service.exception.DuplicateUsernameException;
import br.jeanjacintho.tideflow.user_service.exception.InvalidPasswordException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.Department;
import br.jeanjacintho.tideflow.user_service.model.SystemRole;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.DepartmentRepository;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;
import br.jeanjacintho.tideflow.user_service.specifiction.UserSpecification;

@Service
public class UserService {
    
    private static final int NUMERIC_PASSWORD_MIN = 100000;
    private static final int NUMERIC_PASSWORD_MAX = 999999;
    private static final int NUMERIC_PASSWORD_LENGTH = 6;
    private static final int USERNAME_GENERATION_MAX_ATTEMPTS = 100;
    private static final int NUMERIC_USERNAME_MAX_ATTEMPTS = 1000;
    private static final int COMPANY_PREFIX_LENGTH = 4;
    private static final int SEQUENTIAL_NUMBER_LENGTH = 4;
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyAdminRepository companyAdminRepository;
    private final UsageTrackingService usageTrackingService;
    private final SubscriptionService subscriptionService;
    private final SecureRandom secureRandom;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService,
                      CompanyRepository companyRepository, DepartmentRepository departmentRepository,
                      CompanyAdminRepository companyAdminRepository,
                      UsageTrackingService usageTrackingService, SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.companyAdminRepository = companyAdminRepository;
        this.usageTrackingService = usageTrackingService;
        this.subscriptionService = subscriptionService;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public UserResponseDTO register(RegisterDTO registerDTO) {
        String username = resolveUsername(registerDTO.username(), null);
        validateUsernameAndEmail(username, registerDTO.email());

        User user = new User();
        user.setName(registerDTO.name());
        user.setUsername(username);
        user.setEmail(registerDTO.email());
        user.setPassword(passwordEncoder.encode(registerDTO.password()));

        User savedUser = userRepository.save(user);
        sendWelcomeEmailIfProvided(savedUser);
        
        return UserResponseDTO.fromEntity(savedUser);
    }
    
    private String generateUniqueUsername() {
        String username;
        int attempts = 0;
        do {
            username = "user_" + System.currentTimeMillis() + "_" + 
                      String.valueOf(Math.abs(UUID.randomUUID().hashCode())).substring(0, 6);
            attempts++;
            if (attempts > USERNAME_GENERATION_MAX_ATTEMPTS) {
                throw new RuntimeException("Não foi possível gerar username único após " + USERNAME_GENERATION_MAX_ATTEMPTS + " tentativas");
            }
        } while (userRepository.existsByUsername(username));
        
        return username;
    }

    @Transactional
    public UserResponseDTO createUser(CreateUserRequestDTO requestDTO) {
        String username = resolveUsername(requestDTO.getUsername(), null);
        validateUsernameAndEmail(username, requestDTO.getEmail());

        User user = new User();
        user.setName(requestDTO.getName());
        user.setUsername(username);
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setPhone(requestDTO.getPhone());
        user.setAvatarUrl(requestDTO.getAvatarUrl());
        user.setCity(requestDTO.getCity());
        user.setState(requestDTO.getState());

        User savedUser = userRepository.save(user);
        sendWelcomeEmailIfProvided(savedUser);
        
        return UserResponseDTO.fromEntity(savedUser, companyAdminRepository);
    }

    public UserResponseDTO findById(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        
        validateTenantAccess(user);
        
        return UserResponseDTO.fromEntity(user, companyAdminRepository);
    }

    public Page<UserResponseDTO> findAll(String name, String email, String phone, String city, String state, @NonNull Pageable pageable) {
        Specification<User> specification = UserSpecification.withFilters(name, email, phone, city, state);
        
        // Adiciona filtro de company_id se estiver no contexto (multi-tenant)
        // SYSTEM_ADMIN não tem filtro - vê todos os usuários de todas as empresas
        if (!TenantContext.isSystemAdmin()) {
            UUID currentCompanyId = TenantContext.getCompanyId();
            if (currentCompanyId != null) {
                specification = specification.and((root, query, cb) -> 
                    cb.equal(root.get("company").get("id"), currentCompanyId));
            }
        }
        
        return userRepository.findAll(specification, pageable)
                .map(user -> UserResponseDTO.fromEntity(user, companyAdminRepository));
    }

    @Transactional
    public UserResponseDTO updateUser(@NonNull UUID id, UpdateUserRequestDTO requestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        validateTenantAccess(existingUser);
        validateEmailUpdate(existingUser, requestDTO.getEmail());

        existingUser.setName(requestDTO.getName());
        if (StringUtils.hasText(requestDTO.getEmail())) {
            existingUser.setEmail(requestDTO.getEmail());
        }
        if (StringUtils.hasText(requestDTO.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        }
        existingUser.setPhone(requestDTO.getPhone());
        existingUser.setAvatarUrl(requestDTO.getAvatarUrl());
        existingUser.setTrustedEmail(requestDTO.getTrustedEmail());
        existingUser.setCity(requestDTO.getCity());
        existingUser.setState(requestDTO.getState());

        User updatedUser = userRepository.save(existingUser);
        return UserResponseDTO.fromEntity(updatedUser, companyAdminRepository);
    }

    @Transactional
    public void deleteUser(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        
        validateTenantAccess(user);
        
        userRepository.deleteById(id);
    }

    public Optional<UserResponseDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> UserResponseDTO.fromEntity(user, companyAdminRepository));
    }

    @Transactional
    public InviteUserResponseDTO inviteUser(@NonNull UUID companyId, InviteUserRequestDTO requestDTO) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        Department department = departmentRepository.findByIdAndCompanyId(requestDTO.departmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", requestDTO.departmentId()));

        validateCompanyAccess(companyId);
        
        // Valida limites de uso do plano
        usageTrackingService.validateUsageLimits(companyId);
        
        String username = resolveUsername(requestDTO.username(), companyId);
        validateUsernameAndEmail(username, requestDTO.email());
        validateEmployeeId(companyId, requestDTO.employeeId());

        String temporaryPassword = generateNumericPassword();
        
        User user = new User();
        user.setName(requestDTO.name());
        user.setUsername(username);
        user.setEmail(requestDTO.email());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setCompany(company);
        user.setDepartment(department);
        user.setEmployeeId(requestDTO.employeeId());
        user.setSystemRole(SystemRole.NORMAL);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);
        
        // Atualiza contagem de usuários na assinatura
        subscriptionService.updateUserCount(companyId);

        return new InviteUserResponseDTO(
            savedUser.getId(),
            savedUser.getUsername(),
            temporaryPassword,
            "Usuário criado com sucesso. Passe as credenciais pessoalmente ao funcionário."
        );
    }

    @Transactional
    public UserResponseDTO createCompanyUser(@NonNull UUID companyId, CreateCompanyUserRequestDTO requestDTO) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        Department department = departmentRepository.findByIdAndCompanyId(requestDTO.departmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", requestDTO.departmentId()));

        validateCompanyAccess(companyId);
        
        // Valida limites de uso do plano ANTES de criar o usuário
        // Força flush para garantir que a contagem esteja atualizada
        userRepository.flush();
        usageTrackingService.validateUsageLimits(companyId);
        
        String username = resolveUsername(requestDTO.username(), companyId);
        validateUsernameAndEmail(username, requestDTO.email());
        validateEmployeeId(companyId, requestDTO.employeeId());
        
        User user = new User();
        user.setName(requestDTO.name());
        user.setUsername(username);
        user.setEmail(requestDTO.email());
        user.setPassword(passwordEncoder.encode(requestDTO.password()));
        user.setCompany(company);
        user.setDepartment(department);
        user.setEmployeeId(requestDTO.employeeId());
        user.setPhone(requestDTO.phone());
        user.setCity(requestDTO.city());
        user.setState(requestDTO.state());
        user.setSystemRole(SystemRole.NORMAL);
        user.setMustChangePassword(false);
        user.setIsActive(true); // Garante que o usuário está ativo

        User savedUser = userRepository.save(user);
        
        // Atualiza contagem de usuários na assinatura
        subscriptionService.updateUserCount(companyId);

        return UserResponseDTO.fromEntity(savedUser, companyAdminRepository);
    }

    private String generateNumericUsername(UUID companyId) {
        // Gera username numérico baseado no ID da empresa + número sequencial
        // Formato: {companyId últimos 4 dígitos}{número sequencial de 4 dígitos}
        String companyIdStr = companyId.toString().replace("-", "");
        String companyPrefix = companyIdStr.substring(Math.max(0, companyIdStr.length() - COMPANY_PREFIX_LENGTH));
        
        // Usa count para evitar carregar todos os usuários
        Long userCount = userRepository.countActiveUsersByCompanyId(companyId);
        int nextNumber = userCount.intValue() + 1;
        
        // Garante que o username seja único
        String username;
        int attempts = 0;
        do {
            username = companyPrefix + String.format("%0" + SEQUENTIAL_NUMBER_LENGTH + "d", nextNumber + attempts);
            attempts++;
            if (attempts > NUMERIC_USERNAME_MAX_ATTEMPTS) {
                // Fallback: usa timestamp
                username = companyPrefix + String.valueOf(System.currentTimeMillis()).substring(8);
                break;
            }
        } while (userRepository.existsByUsername(username));
        
        return username;
    }

    private String generateNumericPassword() {
        // Gera senha temporária numérica (6 dígitos) usando SecureRandom para maior segurança
        int password = NUMERIC_PASSWORD_MIN + secureRandom.nextInt(NUMERIC_PASSWORD_MAX - NUMERIC_PASSWORD_MIN + 1);
        return String.format("%0" + NUMERIC_PASSWORD_LENGTH + "d", password);
    }

    public List<UserResponseDTO> getUsersByCompany(@NonNull UUID companyId) {
        validateCompanyAccess(companyId);

        return userRepository.findByCompanyId(companyId).stream()
                .map(user -> UserResponseDTO.fromEntity(user, companyAdminRepository))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<UserResponseDTO> getUsersByDepartment(@NonNull UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", departmentId));

        UUID companyId = department.getCompany().getId();
        validateCompanyAccess(companyId);

        return userRepository.findByDepartmentId(departmentId).stream()
                .map(user -> UserResponseDTO.fromEntity(user, companyAdminRepository))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDTO requestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }

        User user = findUserByUsernameOrEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", authentication.getName()));

        // Verifica se a senha atual está correta
        if (!passwordEncoder.matches(requestDTO.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Senha atual incorreta");
        }

        // Verifica se a nova senha é diferente da atual
        if (passwordEncoder.matches(requestDTO.newPassword(), user.getPassword())) {
            throw new InvalidPasswordException("A nova senha deve ser diferente da senha atual");
        }

        // Atualiza a senha e remove a flag de obrigatoriedade de alteração
        user.setPassword(passwordEncoder.encode(requestDTO.newPassword()));
        user.setMustChangePassword(false);

        userRepository.save(user);
    }

    // Métodos privados auxiliares

    private Optional<User> findUserByUsernameOrEmail(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier);
    }

    private String resolveUsername(String providedUsername, UUID companyId) {
        if (StringUtils.hasText(providedUsername)) {
            return providedUsername;
        }
        return companyId != null 
            ? generateNumericUsername(companyId) 
            : generateUniqueUsername();
    }

    private void validateUsernameAndEmail(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }
        
        if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    private void validateEmailUpdate(User existingUser, String newEmail) {
        if (!StringUtils.hasText(newEmail)) {
            return;
        }
        
        String currentEmail = StringUtils.hasText(existingUser.getEmail()) ? existingUser.getEmail() : "";
        if (!currentEmail.equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateEmailException(newEmail);
        }
    }

    private void validateEmployeeId(UUID companyId, String employeeId) {
        if (StringUtils.hasText(employeeId) && userRepository.existsByCompanyIdAndEmployeeId(companyId, employeeId)) {
            throw new IllegalArgumentException("Employee ID já está em uso nesta empresa: " + employeeId);
        }
    }

    private void validateCompanyAccess(UUID companyId) {
        UUID currentCompanyId = TenantContext.getCompanyId();
        if (!TenantContext.isSystemAdmin() && (currentCompanyId == null || !currentCompanyId.equals(companyId))) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }
    }

    private void validateTenantAccess(User user) {
        if (TenantContext.isSystemAdmin()) {
            return;
        }
        
        UUID currentCompanyId = TenantContext.getCompanyId();
        if (currentCompanyId != null && user.getCompany() != null && 
            !currentCompanyId.equals(user.getCompany().getId())) {
            throw new ResourceNotFoundException("Usuário", user.getId());
        }
    }

    private void sendWelcomeEmailIfProvided(User user) {
        if (StringUtils.hasText(user.getEmail())) {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        }
    }
}
