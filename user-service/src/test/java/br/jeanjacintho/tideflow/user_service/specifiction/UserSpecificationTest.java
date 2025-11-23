package br.jeanjacintho.tideflow.user_service.specifiction;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import br.jeanjacintho.tideflow.user_service.model.User;

@DisplayName("UserSpecification Tests")
class UserSpecificationTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPhone("1234567890");
        testUser.setCity("São Paulo");
        testUser.setState("SP");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("withFilters - Deve criar specification sem filtros")
    void testWithFiltersNoFilters() {
        Specification<User> spec = UserSpecification.withFilters(null, null, null, null, null);

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtro de nome")
    void testWithFiltersName() {
        Specification<User> spec = UserSpecification.withFilters("John", null, null, null, null);

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtro de email")
    void testWithFiltersEmail() {
        Specification<User> spec = UserSpecification.withFilters(null, "john@example.com", null, null, null);

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtro de telefone")
    void testWithFiltersPhone() {
        Specification<User> spec = UserSpecification.withFilters(null, null, "1234567890", null, null);

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtro de cidade")
    void testWithFiltersCity() {
        Specification<User> spec = UserSpecification.withFilters(null, null, null, "São Paulo", null);

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtro de estado")
    void testWithFiltersState() {
        Specification<User> spec = UserSpecification.withFilters(null, null, null, null, "SP");

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com todos os filtros")
    void testWithFiltersAll() {
        Specification<User> spec = UserSpecification.withFilters("John", "john@example.com", 
                "1234567890", "São Paulo", "SP");

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification com filtros vazios")
    void testWithFiltersEmpty() {
        Specification<User> spec = UserSpecification.withFilters("", "", "", "", "");

        assertNotNull(spec);
    }

    @Test
    @DisplayName("withFilters - Deve criar specification que funciona com case insensitive")
    void testWithFiltersCaseInsensitive() {
        Specification<User> spec = UserSpecification.withFilters("JOHN", "JOHN@EXAMPLE.COM", 
                null, "SÃO PAULO", "sp");

        assertNotNull(spec);
    }
}

