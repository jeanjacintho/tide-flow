package br.jeanjacintho.tideflow.user_service.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.jeanjacintho.tideflow.user_service.config.TestSecurityConfig;
import br.jeanjacintho.tideflow.user_service.config.TokenService;
import br.jeanjacintho.tideflow.user_service.service.UserService;

@WebMvcTest(controllers = TestController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("GlobalExceptionHandler Tests")
@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Deve tratar ResourceNotFoundException corretamente")
    void testResourceNotFoundException() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(userService.findById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Usuário", nonExistentId));

        mockMvc.perform(get("/test/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("Deve tratar DuplicateEmailException corretamente")
    void testDuplicateEmailException() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new DuplicateEmailException("test@example.com"));

        mockMvc.perform(post("/test/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"name\":\"Test\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Deve tratar ValidationException corretamente")
    void testValidationException() throws Exception {
        ValidationException ex = new ValidationException(java.util.Arrays.asList("Campo obrigatório", "Formato inválido"));

        when(userService.createUser(any()))
                .thenThrow(ex);

        mockMvc.perform(post("/test/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"name\":\"Test User\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException corretamente")
    void testMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(post("/test/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"invalid-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException corretamente")
    void testIllegalArgumentException() throws Exception {
        when(userService.findById(any()))
                .thenThrow(new IllegalArgumentException("Argumento inválido"));

        mockMvc.perform(get("/test/users/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Deve tratar Exception genérica corretamente")
    void testGenericException() throws Exception {
        when(userService.findById(any()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/test/users/{id}", UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/test")
@SuppressWarnings("null")
class TestController {

    @org.springframework.beans.factory.annotation.Autowired
    private UserService userService;

    @org.springframework.web.bind.annotation.GetMapping("/users/{id}")
    public br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO getUser(
            @org.springframework.web.bind.annotation.PathVariable UUID id) {
        return userService.findById(id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/users")
    public br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO createUser(
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO request) {
        return userService.createUser(request);
    }
}
