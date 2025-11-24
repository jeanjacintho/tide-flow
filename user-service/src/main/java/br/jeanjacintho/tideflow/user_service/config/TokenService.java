package br.jeanjacintho.tideflow.user_service.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;

@Service
public class TokenService {
    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private CompanyAdminRepository companyAdminRepository;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            
            // Usa username como subject, fallback para email se username não existir (compatibilidade)
            String subject = user.getUsername() != null ? user.getUsername() : user.getEmail();
            
            var builder = JWT.create()
                    .withIssuer("tideflow-user-service")
                    .withSubject(subject)
                    .withClaim("user_id", user.getId().toString())
                    .withExpiresAt(generateExpirationDate());
            
            // Adiciona informações de tenant se o usuário tiver empresa
            if (user.getCompany() != null) {
                builder.withClaim("company_id", user.getCompany().getId().toString());
            }
            
            if (user.getDepartment() != null) {
                builder.withClaim("department_id", user.getDepartment().getId().toString());
            }
            
            // Busca role do usuário na empresa (CompanyAdmin)
            if (user.getCompany() != null) {
                companyAdminRepository.findByUserIdAndCompanyId(user.getId(), user.getCompany().getId())
                        .ifPresent(admin -> {
                            builder.withClaim("company_role", admin.getRole().name());
                            if (admin.getPermissions() != null) {
                                builder.withClaim("permissions", admin.getPermissions());
                            }
                        });
            }
            
            // Adiciona system_role (SYSTEM_ADMIN tem acesso total)
            if (user.getSystemRole() != null) {
                builder.withClaim("system_role", user.getSystemRole().name());
            }
            
            return builder.sign(algorithm);
        } catch(JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar token", e);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            return decodedJWT.getSubject();
        } catch(JWTVerificationException e) {
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Extrai o company_id do token JWT.
     */
    public UUID getCompanyIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            
            String companyIdStr = decodedJWT.getClaim("company_id").asString();
            return companyIdStr != null ? UUID.fromString(companyIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai o department_id do token JWT.
     */
    public UUID getDepartmentIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            
            String departmentIdStr = decodedJWT.getClaim("department_id").asString();
            return departmentIdStr != null ? UUID.fromString(departmentIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai o company_role do token JWT.
     */
    public String getCompanyRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            
            return decodedJWT.getClaim("company_role").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai o system_role do token JWT.
     */
    public String getSystemRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            
            return decodedJWT.getClaim("system_role").asString();
        } catch (Exception e) {
            return null;
        }
    }

    private Instant generateExpirationDate(){
        return Instant.now().plus(2, ChronoUnit.HOURS);
    }
}
