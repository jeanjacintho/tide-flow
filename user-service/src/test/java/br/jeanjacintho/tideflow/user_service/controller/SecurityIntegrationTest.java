package br.jeanjacintho.tideflow.user_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.jeanjacintho.tideflow.user_service.config.TestSecurityConfig;
import br.jeanjacintho.tideflow.user_service.config.TokenService;
import br.jeanjacintho.tideflow.user_service.dto.request.AuthenticationDTO;
import br.jeanjacintho.tideflow.user_service.exception.GlobalExceptionHandler;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;
import br.jeanjacintho.tideflow.user_service.service.UserService;

@WebMvcTest(controllers = {AuthController.class, UserController.class}, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("Security Integration Tests")
@SuppressWarnings("null")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setName("Test User");
    }

    @Test
    @DisplayName("GET /users - Deve permitir acesso sem autenticação (test security config)")
    void testGetUsersWithoutAuth() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/login - Deve permitir acesso sem autenticação")
    void testLoginWithoutAuth() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("test@example.com", "password123");
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities("ROLE_USER")
                .build();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameOrEmailWithCompanyAndDepartment("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenService.generateToken(any(User.class)))
                .thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/register - Deve permitir acesso sem autenticação")
    void testRegisterWithoutAuth() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New User\",\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }
}

