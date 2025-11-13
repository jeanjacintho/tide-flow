package br.jeanjacintho.tideflow.user_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.jeanjacintho.tideflow.user_service.config.TestSecurityConfig;
import br.jeanjacintho.tideflow.user_service.config.TokenService;
import br.jeanjacintho.tideflow.user_service.dto.request.AuthenticationDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.RegisterDTO;
import br.jeanjacintho.tideflow.user_service.exception.GlobalExceptionHandler;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.model.UserRole;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(java.util.UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setName("Test User");

        testToken = "test-jwt-token-12345";
    }

    @Test
    @DisplayName("POST /auth/login - Deve fazer login com sucesso")
    void testLoginSuccess() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("test@example.com", "password123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                createUserDetails(testUser), null, createUserDetails(testUser).getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenService.generateToken(any(User.class)))
                .thenReturn(testToken);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(testToken));
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 401 com credenciais inválidas")
    void testLoginInvalidCredentials() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("test@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 400 com email vazio")
    void testLoginEmptyEmail() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("", "password123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 400 com senha vazia")
    void testLoginEmptyPassword() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("test@example.com", "");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 400 com corpo da requisição inválido")
    void testLoginInvalidBody() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve registrar usuário com sucesso")
    void testRegisterSuccess() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO("newuser@example.com", "password123", UserRole.USER);

        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 com email duplicado")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO("existing@example.com", "password123", UserRole.USER);

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve registrar usuário ADMIN com sucesso")
    void testRegisterAdminSuccess() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO("admin@example.com", "password123", UserRole.ADMIN);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 com email vazio")
    void testRegisterEmptyEmail() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO("", "password123", UserRole.USER);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 com senha vazia")
    void testRegisterEmptyPassword() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO("test@example.com", "", UserRole.USER);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 com corpo da requisição inválido")
    void testRegisterInvalidBody() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 500 quando usuário não encontrado após autenticação")
    void testLoginUserNotFoundAfterAuth() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("test@example.com", "password123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                createUserDetails(testUser), null, createUserDetails(testUser).getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().is5xxServerError());
    }

    private UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}

