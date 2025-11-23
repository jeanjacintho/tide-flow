package br.jeanjacintho.tideflow.user_service.config;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public SecurityFilter(TokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            var token = this.recoverToken(request);
            if(token != null) {
                try {
                    var login = tokenService.validateToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(login);

                    var authHeader = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authHeader);

                    // Extrai informações de tenant do token e armazena no TenantContext
                    UUID companyId = tokenService.getCompanyIdFromToken(token);
                    UUID departmentId = tokenService.getDepartmentIdFromToken(token);
                    String companyRole = tokenService.getCompanyRoleFromToken(token);
                    String systemRole = tokenService.getSystemRoleFromToken(token);

                    if (companyId != null) {
                        TenantContext.setCompanyId(companyId);
                    }
                    if (departmentId != null) {
                        TenantContext.setDepartmentId(departmentId);
                    }
                    if (companyRole != null) {
                        TenantContext.setCompanyRole(companyRole);
                    }
                    if (systemRole != null) {
                        TenantContext.setSystemRole(systemRole);
                    }
                } catch (Exception e) {
                    // Token inválido ou usuário não encontrado - continua sem autenticação
                }
            }
        } finally {
            // Garante que o contexto é limpo após o processamento
            // Isso será feito no TenantFilter que será executado depois
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}
