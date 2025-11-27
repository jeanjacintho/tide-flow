package br.jeanjacintho.tideflow.ai_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    
    private final TokenValidationService tokenValidationService;

    public SecurityFilter(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Endpoints públicos não precisam de autenticação
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = recoverToken(request);
        if (token != null) {
            try {
                String username = tokenValidationService.validateToken(token);
                
                // Cria UserDetails básico
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("")
                        .authorities(new ArrayList<>())
                        .build();
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Armazena informações do token no request para uso posterior
                UUID companyId = tokenValidationService.getCompanyIdFromToken(token);
                UUID departmentId = tokenValidationService.getDepartmentIdFromToken(token);
                String companyRole = tokenValidationService.getCompanyRoleFromToken(token);
                String systemRole = tokenValidationService.getSystemRoleFromToken(token);
                
                request.setAttribute("companyId", companyId);
                request.setAttribute("departmentId", departmentId);
                request.setAttribute("companyRole", companyRole);
                request.setAttribute("systemRole", systemRole);
                
            } catch (Exception e) {
                // Token inválido - continua sem autenticação
                // O Spring Security vai bloquear se necessário
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }

    private boolean isPublicEndpoint(String path) {
        if (path == null) {
            return false;
        }
        // Adicione endpoints públicos aqui se necessário
        return path.equals("/actuator/health") 
            || path.startsWith("/actuator/");
    }
}
