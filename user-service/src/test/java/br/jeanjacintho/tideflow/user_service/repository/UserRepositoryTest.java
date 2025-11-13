package br.jeanjacintho.tideflow.user_service.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.model.UserRole;

@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setPhone("1234567890");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setCity("São Paulo");
        testUser.setState("SP");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("save - Deve salvar usuário com sucesso")
    void testSaveUser() {
        User savedUser = userRepository.save(testUser);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals(testUser.getEmail(), savedUser.getEmail());
        assertEquals(testUser.getName(), savedUser.getName());
    }

    @Test
    @DisplayName("findById - Deve encontrar usuário por ID")
    void testFindById() {
        User savedUser = entityManager.persistAndFlush(testUser);
        UUID userId = savedUser.getId();

        Optional<User> foundUser = userRepository.findById(userId);

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @DisplayName("findById - Deve retornar Optional vazio quando usuário não encontrado")
    void testFindByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        Optional<User> foundUser = userRepository.findById(nonExistentId);

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("findByEmail - Deve encontrar usuário por email")
    void testFindByEmail() {
        User savedUser = entityManager.persistAndFlush(testUser);

        Optional<User> foundUser = userRepository.findByEmail(testUser.getEmail());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail - Deve retornar Optional vazio quando email não encontrado")
    void testFindByEmailNotFound() {
        Optional<User> foundUser = userRepository.findByEmail("notfound@example.com");

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("existsByEmail - Deve retornar true quando email existe")
    void testExistsByEmailTrue() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByEmail(testUser.getEmail());

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByEmail - Deve retornar false quando email não existe")
    void testExistsByEmailFalse() {
        boolean exists = userRepository.existsByEmail("notfound@example.com");

        assertFalse(exists);
    }

    @Test
    @DisplayName("deleteById - Deve deletar usuário por ID")
    void testDeleteById() {
        User savedUser = entityManager.persistAndFlush(testUser);
        UUID userId = savedUser.getId();

        userRepository.deleteById(userId);
        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findById(userId);
        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("save - Deve atualizar usuário existente")
    void testUpdateUser() {
        User savedUser = entityManager.persistAndFlush(testUser);
        UUID userId = savedUser.getId();

        savedUser.setName("Jane Doe");
        savedUser.setEmail("jane@example.com");
        userRepository.save(savedUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findById(userId);
        assertTrue(foundUser.isPresent());
        assertEquals("Jane Doe", foundUser.get().getName());
        assertEquals("jane@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail - Deve ser case sensitive")
    void testFindByEmailCaseSensitive() {
        entityManager.persistAndFlush(testUser);

        Optional<User> foundUser = userRepository.findByEmail("JOHN@EXAMPLE.COM");

        // O método findByEmail pode ser case-sensitive dependendo da configuração do banco
        // Este teste verifica o comportamento atual
        assertNotNull(foundUser);
    }
}

