package br.jeanjacintho.tideflow.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Tests")
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("loadUserByUsername - Deve carregar UserDetails com sucesso")
    void testLoadUserByUsernameSuccess() {
        when(userRepository.findByUsernameOrEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserDetails userDetails = authorizationService.loadUserByUsername(testUser.getEmail());

        assertNotNull(userDetails);
        assertEquals(testUser.getEmail(), userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername - Deve lançar UsernameNotFoundException quando usuário não encontrado")
    void testLoadUserByUsernameNotFound() {
        String nonExistentEmail = "notfound@example.com";
        when(userRepository.findByUsernameOrEmail(nonExistentEmail)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, 
                () -> authorizationService.loadUserByUsername(nonExistentEmail));
    }

    @Test
    @DisplayName("loadUserByUsername - Deve sempre retornar ROLE_USER")
    void testLoadUserByUsernameAlwaysReturnsUserRole() {
        when(userRepository.findByUsernameOrEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserDetails userDetails = authorizationService.loadUserByUsername(testUser.getEmail());

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername - Deve retornar UserDetails com authorities corretas")
    void testLoadUserByUsernameAuthorities() {
        when(userRepository.findByUsernameOrEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserDetails userDetails = authorizationService.loadUserByUsername(testUser.getEmail());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

