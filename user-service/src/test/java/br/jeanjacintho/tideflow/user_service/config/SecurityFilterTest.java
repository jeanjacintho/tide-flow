package br.jeanjacintho.tideflow.user_service.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityFilter Tests")
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    private String validToken;
    private String userEmail;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        validToken = "valid-jwt-token";
        userEmail = "test@example.com";
    }

    @Test
    @DisplayName("doFilterInternal - Deve processar requisição sem token")
    void testDoFilterInternalWithoutToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Deve processar requisição com token válido")
    void testDoFilterInternalWithValidToken() throws ServletException, IOException {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userEmail)
                .password("password")
                .authorities("ROLE_USER")
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateToken(validToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService).validateToken(validToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Deve continuar mesmo com token inválido")
    void testDoFilterInternalWithInvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenService.validateToken("invalid-token"))
                .thenThrow(new RuntimeException("Token inválido"));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService).validateToken("invalid-token");
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("doFilterInternal - Deve continuar mesmo quando usuário não encontrado")
    void testDoFilterInternalUserNotFound() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateToken(validToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService).validateToken(validToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Deve processar token sem prefixo Bearer")
    void testDoFilterInternalTokenWithoutBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(validToken);

        securityFilter.doFilterInternal(request, response, filterChain);

        // O filtro deve tentar remover "Bearer " mesmo que não exista
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("recoverToken - Deve extrair token do header Authorization")
    void testRecoverToken() throws ServletException, IOException {
        String tokenWithBearer = "Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(tokenWithBearer);
        when(tokenService.validateToken(validToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail))
                .thenReturn(org.springframework.security.core.userdetails.User.builder()
                        .username(userEmail)
                        .password("password")
                        .authorities("ROLE_USER")
                        .build());

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService).validateToken(validToken);
    }
}

