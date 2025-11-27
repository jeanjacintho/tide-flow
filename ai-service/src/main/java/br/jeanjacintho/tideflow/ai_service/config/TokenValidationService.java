package br.jeanjacintho.tideflow.ai_service.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TokenValidationService {
    
    @Value("${jwt.secret:default-secret-key-change-in-production}")
    private String jwtSecret;

    /**
     * Valida um token JWT e retorna o subject (username).
     */
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * Extrai o company_id do token JWT.
     */
    public UUID getCompanyIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
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
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
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
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
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
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("tideflow-user-service")
                    .build()
                    .verify(token);
            
            return decodedJWT.getClaim("system_role").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica se o usuário tem permissão para acessar relatórios corporativos.
     * OWNER, ADMIN, HR_MANAGER e SYSTEM_ADMIN podem acessar.
     */
    public boolean canAccessCorporateReports(String token) {
        try {
            String systemRole = getSystemRoleFromToken(token);
            if ("SYSTEM_ADMIN".equals(systemRole)) {
                return true;
            }
            
            String companyRole = getCompanyRoleFromToken(token);
            if (companyRole == null) {
                return false;
            }
            
            return "OWNER".equals(companyRole) 
                || "ADMIN".equals(companyRole) 
                || "HR_MANAGER".equals(companyRole);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se o companyId do token corresponde ao companyId solicitado.
     */
    public boolean canAccessCompany(String token, UUID companyId) {
        try {
            String systemRole = getSystemRoleFromToken(token);
            if ("SYSTEM_ADMIN".equals(systemRole)) {
                return true;
            }
            
            UUID tokenCompanyId = getCompanyIdFromToken(token);
            return tokenCompanyId != null && tokenCompanyId.equals(companyId);
        } catch (Exception e) {
            return false;
        }
    }
}
