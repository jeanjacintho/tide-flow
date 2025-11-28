package br.jeanjacintho.tideflow.user_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.jeanjacintho.tideflow.user_service.config.TestSecurityConfig;
import br.jeanjacintho.tideflow.user_service.config.TokenService;
import br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpdateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.DuplicateEmailException;
import br.jeanjacintho.tideflow.user_service.exception.GlobalExceptionHandler;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UserController Tests")
@SuppressWarnings("null")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private UserResponseDTO testUserResponseDTO;
    private UUID testUserId;
    private CreateUserRequestDTO createUserRequestDTO;
    private UpdateUserRequestDTO updateUserRequestDTO;

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
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);

        testUserResponseDTO = UserResponseDTO.fromEntity(testUser);

        createUserRequestDTO = new CreateUserRequestDTO();
        createUserRequestDTO.setName("John Doe");
        createUserRequestDTO.setEmail("john@example.com");
        createUserRequestDTO.setPassword("password123");
        createUserRequestDTO.setPhone("1234567890");
        createUserRequestDTO.setAvatarUrl("https://example.com/avatar.jpg");
        createUserRequestDTO.setCity("São Paulo");
        createUserRequestDTO.setState("SP");

        updateUserRequestDTO = new UpdateUserRequestDTO();
        updateUserRequestDTO.setName("Jane Doe");
        updateUserRequestDTO.setEmail("jane@example.com");
        updateUserRequestDTO.setPhone("9876543210");
        updateUserRequestDTO.setCity("Rio de Janeiro");
        updateUserRequestDTO.setState("RJ");
    }

    @Test
    @DisplayName("POST /users - Deve criar usuário com sucesso")
    void testCreateUserSuccess() throws Exception {
        when(userService.createUser(any(CreateUserRequestDTO.class)))
                .thenReturn(testUserResponseDTO);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"));
    }

    @Test
    @DisplayName("POST /users - Deve retornar 409 com email duplicado")
    void testCreateUserDuplicateEmail() throws Exception {
        when(userService.createUser(any(CreateUserRequestDTO.class)))
                .thenThrow(new DuplicateEmailException("john@example.com"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com nome vazio")
    void testCreateUserEmptyName() throws Exception {
        createUserRequestDTO.setName("");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com nome muito curto")
    void testCreateUserNameTooShort() throws Exception {
        createUserRequestDTO.setName("A");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com nome muito longo")
    void testCreateUserNameTooLong() throws Exception {
        createUserRequestDTO.setName("A".repeat(101));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com email inválido")
    void testCreateUserInvalidEmail() throws Exception {
        createUserRequestDTO.setEmail("invalid-email");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com senha muito curta")
    void testCreateUserPasswordTooShort() throws Exception {
        createUserRequestDTO.setPassword("short");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com telefone muito longo")
    void testCreateUserPhoneTooLong() throws Exception {
        createUserRequestDTO.setPhone("1".repeat(21));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve retornar 400 com estado muito longo")
    void testCreateUserStateTooLong() throws Exception {
        createUserRequestDTO.setState("ABC");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users/{id} - Deve retornar usuário por ID com sucesso")
    void testGetUserByIdSuccess() throws Exception {
        when(userService.findById(testUserId))
                .thenReturn(testUserResponseDTO);

        mockMvc.perform(get("/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /users/{id} - Deve retornar 404 quando usuário não encontrado")
    void testGetUserByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(userService.findById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Usuário", nonExistentId));

        mockMvc.perform(get("/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /users/{id} - Deve retornar 400 com ID inválido")
    void testGetUserByIdInvalidId() throws Exception {
        mockMvc.perform(get("/users/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users - Deve retornar lista paginada de usuários")
    void testGetAllUsersSuccess() throws Exception {
        List<UserResponseDTO> users = new ArrayList<>();
        users.add(testUserResponseDTO);

        Page<UserResponseDTO> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        when(userService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testUserId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /users - Deve filtrar usuários por nome")
    void testGetAllUsersWithNameFilter() throws Exception {
        List<UserResponseDTO> users = new ArrayList<>();
        users.add(testUserResponseDTO);

        Page<UserResponseDTO> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        when(userService.findAll(eq("John"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users")
                .param("name", "John")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /users - Deve filtrar usuários por email")
    void testGetAllUsersWithEmailFilter() throws Exception {
        List<UserResponseDTO> users = new ArrayList<>();
        users.add(testUserResponseDTO);

        Page<UserResponseDTO> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        when(userService.findAll(any(), eq("john@example.com"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users")
                .param("email", "john@example.com")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /users - Deve filtrar usuários por múltiplos filtros")
    void testGetAllUsersWithMultipleFilters() throws Exception {
        List<UserResponseDTO> users = new ArrayList<>();
        users.add(testUserResponseDTO);

        Page<UserResponseDTO> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        when(userService.findAll(eq("John"), eq("john@example.com"), eq("1234567890"), eq("São Paulo"), eq("SP"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users")
                .param("name", "John")
                .param("email", "john@example.com")
                .param("phone", "1234567890")
                .param("city", "São Paulo")
                .param("state", "SP")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve atualizar usuário com sucesso")
    void testUpdateUserSuccess() throws Exception {
        UserResponseDTO updatedResponse = UserResponseDTO.fromEntity(testUser);
        updatedResponse.setName("Jane Doe");
        updatedResponse.setEmail("jane@example.com");

        when(userService.updateUser(eq(testUserId), any(UpdateUserRequestDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve retornar 404 quando usuário não encontrado")
    void testUpdateUserNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(userService.updateUser(eq(nonExistentId), any(UpdateUserRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Usuário", nonExistentId));

        mockMvc.perform(put("/users/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve retornar 409 com email duplicado")
    void testUpdateUserDuplicateEmail() throws Exception {
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequestDTO.class)))
                .thenThrow(new DuplicateEmailException("jane@example.com"));

        mockMvc.perform(put("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve retornar 400 com validações inválidas")
    void testUpdateUserInvalidValidation() throws Exception {
        updateUserRequestDTO.setName("");

        mockMvc.perform(put("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve atualizar usuário sem senha")
    void testUpdateUserWithoutPassword() throws Exception {
        updateUserRequestDTO.setPassword(null);
        UserResponseDTO updatedResponse = UserResponseDTO.fromEntity(testUser);
        updatedResponse.setName("Jane Doe");

        when(userService.updateUser(eq(testUserId), any(UpdateUserRequestDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /users/{id} - Deve deletar usuário com sucesso")
    void testDeleteUserSuccess() throws Exception {
        doNothing().when(userService).deleteUser(testUserId);

        mockMvc.perform(delete("/users/{id}", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{id} - Deve retornar 404 quando usuário não encontrado")
    void testDeleteUserNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Usuário", nonExistentId))
                .when(userService).deleteUser(nonExistentId);

        mockMvc.perform(delete("/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /users/{id} - Deve retornar 400 com ID inválido")
    void testDeleteUserInvalidId() throws Exception {
        mockMvc.perform(delete("/users/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - Deve criar usuário com campos opcionais nulos")
    void testCreateUserWithNullOptionalFields() throws Exception {
        createUserRequestDTO.setPhone(null);
        createUserRequestDTO.setAvatarUrl(null);
        createUserRequestDTO.setCity(null);
        createUserRequestDTO.setState(null);

        when(userService.createUser(any(CreateUserRequestDTO.class)))
                .thenReturn(testUserResponseDTO);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /users - Deve retornar página vazia quando não há usuários")
    void testGetAllUsersEmptyPage() throws Exception {
        Page<UserResponseDTO> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(userService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
