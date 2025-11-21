package br.jeanjacintho.tideflow.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.RegisterDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpdateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.DuplicateEmailException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.model.UserRole;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private CreateUserRequestDTO createRequestDTO;
    private UpdateUserRequestDTO updateRequestDTO;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setPhone("1234567890");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setCity("São Paulo");
        testUser.setState("SP");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);

        createRequestDTO = new CreateUserRequestDTO();
        createRequestDTO.setName("John Doe");
        createRequestDTO.setEmail("john@example.com");
        createRequestDTO.setPassword("password123");
        createRequestDTO.setPhone("1234567890");
        createRequestDTO.setAvatarUrl("https://example.com/avatar.jpg");
        createRequestDTO.setCity("São Paulo");
        createRequestDTO.setState("SP");

        updateRequestDTO = new UpdateUserRequestDTO();
        updateRequestDTO.setName("Jane Doe");
        updateRequestDTO.setEmail("jane@example.com");
        updateRequestDTO.setPhone("9876543210");
        updateRequestDTO.setCity("Rio de Janeiro");
        updateRequestDTO.setState("RJ");
    }

    @Test
    @DisplayName("createUser - Deve criar usuário com sucesso")
    void testCreateUserSuccess() {
        when(userRepository.existsByEmail(createRequestDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createRequestDTO.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDTO result = userService.createUser(createRequestDTO);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).existsByEmail(createRequestDTO.getEmail());
        verify(passwordEncoder).encode(createRequestDTO.getPassword());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(testUser.getEmail(), testUser.getName());
    }

    @Test
    @DisplayName("createUser - Deve lançar DuplicateEmailException quando email já existe")
    void testCreateUserDuplicateEmail() {
        when(userRepository.existsByEmail(createRequestDTO.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(createRequestDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("findById - Deve retornar usuário quando encontrado")
    void testFindByIdSuccess() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserResponseDTO result = userService.findById(testUserId);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("findById - Deve lançar ResourceNotFoundException quando usuário não encontrado")
    void testFindByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(nonExistentId));
    }

    @Test
    @DisplayName("findAll - Deve retornar página de usuários")
    void testFindAllSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(java.util.List.of(testUser), pageable, 1);

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(userPage);

        Page<UserResponseDTO> result = userService.findAll(null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
    }

    @Test
    @DisplayName("findAll - Deve aplicar filtros corretamente")
    void testFindAllWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(java.util.List.of(testUser), pageable, 1);

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(userPage);

        Page<UserResponseDTO> result = userService.findAll("John", "john@example.com", "1234567890", "São Paulo", "SP", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
    }

    @Test
    @DisplayName("updateUser - Deve atualizar usuário com sucesso")
    void testUpdateUserSuccess() {
        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setName(updateRequestDTO.getName());
        updatedUser.setEmail(updateRequestDTO.getEmail());
        updatedUser.setPhone(updateRequestDTO.getPhone());
        updatedUser.setCity(updateRequestDTO.getCity());
        updatedUser.setState(updateRequestDTO.getState());
        updatedUser.setPassword(testUser.getPassword());
        updatedUser.setRole(testUser.getRole());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequestDTO.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserResponseDTO result = userService.updateUser(testUserId, updateRequestDTO);

        assertNotNull(result);
        assertEquals(updateRequestDTO.getName(), result.getName());
        assertEquals(updateRequestDTO.getEmail(), result.getEmail());
        verify(userRepository).findById(testUserId);
        verify(userRepository).existsByEmail(updateRequestDTO.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Deve atualizar senha quando fornecida")
    void testUpdateUserWithPassword() {
        updateRequestDTO.setPassword("newPassword123");
        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setName(updateRequestDTO.getName());
        updatedUser.setEmail(updateRequestDTO.getEmail());
        updatedUser.setPassword("$2a$10$newEncodedPassword");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequestDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserResponseDTO result = userService.updateUser(testUserId, updateRequestDTO);

        assertNotNull(result);
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    @DisplayName("updateUser - Não deve atualizar senha quando não fornecida")
    void testUpdateUserWithoutPassword() {
        updateRequestDTO.setPassword(null);
        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setName(updateRequestDTO.getName());
        updatedUser.setEmail(updateRequestDTO.getEmail());
        updatedUser.setPassword(testUser.getPassword());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequestDTO.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        userService.updateUser(testUserId, updateRequestDTO);

        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    @DisplayName("updateUser - Deve lançar ResourceNotFoundException quando usuário não encontrado")
    void testUpdateUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(nonExistentId, updateRequestDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Deve lançar DuplicateEmailException quando novo email já existe")
    void testUpdateUserDuplicateEmail() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequestDTO.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(testUserId, updateRequestDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Deve permitir atualizar com mesmo email")
    void testUpdateUserSameEmail() {
        updateRequestDTO.setEmail(testUser.getEmail());
        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setName(updateRequestDTO.getName());
        updatedUser.setEmail(testUser.getEmail());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserResponseDTO result = userService.updateUser(testUserId, updateRequestDTO);

        assertNotNull(result);
        verify(userRepository, never()).existsByEmail(any(String.class));
    }

    @Test
    @DisplayName("deleteUser - Deve deletar usuário com sucesso")
    void testDeleteUserSuccess() {
        when(userRepository.existsById(testUserId)).thenReturn(true);

        userService.deleteUser(testUserId);

        verify(userRepository).existsById(testUserId);
        verify(userRepository).deleteById(testUserId);
    }

    @Test
    @DisplayName("deleteUser - Deve lançar ResourceNotFoundException quando usuário não encontrado")
    void testDeleteUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(nonExistentId));
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("findByEmail - Deve retornar Optional com usuário quando encontrado")
    void testFindByEmailSuccess() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        Optional<UserResponseDTO> result = userService.findByEmail(testUser.getEmail());

        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail - Deve retornar Optional vazio quando usuário não encontrado")
    void testFindByEmailNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Optional<UserResponseDTO> result = userService.findByEmail("notfound@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("register - Deve registrar usuário e publicar evento")
    void testRegisterSuccess() {
        RegisterDTO registerDTO = new RegisterDTO("John Doe", "john@example.com", "password123", UserRole.USER);
        when(userRepository.existsByEmail(registerDTO.email())).thenReturn(false);
        when(passwordEncoder.encode(registerDTO.password())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDTO result = userService.register(registerDTO);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).existsByEmail(registerDTO.email());
        verify(passwordEncoder).encode(registerDTO.password());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(testUser.getEmail(), testUser.getName());
    }

    @Test
    @DisplayName("createUser - Deve publicar evento com dados corretos")
    void testCreateUserPublishesEvent() {
        when(userRepository.existsByEmail(createRequestDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createRequestDTO.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.createUser(createRequestDTO);

        verify(emailService).sendWelcomeEmail(testUser.getEmail(), testUser.getName());
    }
}
