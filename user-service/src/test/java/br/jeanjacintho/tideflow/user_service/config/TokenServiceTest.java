package br.jeanjacintho.tideflow.user_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.jeanjacintho.tideflow.user_service.model.User;

@DisplayName("TokenService Tests")
class TokenServiceTest {

    private TokenService tokenService;

    private User testUser;
    private static final String TEST_SECRET = "test-secret-key-minimum-256-bits-for-hmac-sha256-algorithm-security";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("generateToken - Deve gerar token JWT válido")
    void testGenerateTokenSuccess() {
        String token = tokenService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("generateToken - Deve gerar token diferente para usuários diferentes")
    void testGenerateTokenDifferentUsers() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("user1@example.com");
        user1.setName("User 1");
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("user2@example.com");
        user2.setName("User 2");
        user2.setUsername("user2");

        String token1 = tokenService.generateToken(user1);
        String token2 = tokenService.generateToken(user2);

        assertNotNull(token1);
        assertNotNull(token2);

    }

    @Test
    @DisplayName("validateToken - Deve validar token gerado corretamente")
    void testValidateTokenSuccess() {
        String token = tokenService.generateToken(testUser);
        String email = tokenService.validateToken(token);

        assertNotNull(email);
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    @DisplayName("validateToken - Deve lançar RuntimeException com token inválido")
    void testValidateTokenInvalid() {
        String invalidToken = "invalid.token.here";

        assertThrows(RuntimeException.class, () -> tokenService.validateToken(invalidToken));
    }

    @Test
    @DisplayName("validateToken - Deve lançar RuntimeException com token vazio")
    void testValidateTokenEmpty() {
        assertThrows(RuntimeException.class, () -> tokenService.validateToken(""));
    }

    @Test
    @DisplayName("validateToken - Deve lançar RuntimeException com token null")
    void testValidateTokenNull() {
        assertThrows(RuntimeException.class, () -> tokenService.validateToken(null));
    }

    @Test
    @DisplayName("validateToken - Deve lançar RuntimeException com token expirado")
    void testValidateTokenExpired() {

        String validToken = tokenService.generateToken(testUser);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        assertThrows(RuntimeException.class, () -> tokenService.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("generateToken e validateToken - Deve funcionar corretamente")
    void testTokenGenerationAndValidation() {
        String token = tokenService.generateToken(testUser);
        String email = tokenService.validateToken(token);

        assertEquals(testUser.getEmail(), email);

        String token2 = tokenService.generateToken(testUser);
        String email2 = tokenService.validateToken(token2);

        assertEquals(testUser.getEmail(), email2);
    }
}
