package br.jeanjacintho.tideflow.ai_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    public UserInfoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<UserInfo> getUserInfo(String userId, String authToken) {
        try {
            // Tenta buscar por UUID primeiro
            UUID userUuid = null;
            try {
                userUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                // userId não é UUID, pode ser username - busca por username
                return getUserInfoByUsername(userId, authToken);
            }
            
            // Busca por UUID
            String url = userServiceUrl + "/api/users/" + userUuid;
            HttpHeaders headers = new HttpHeaders();
            if (authToken != null) {
                headers.set("Authorization", "Bearer " + authToken);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();
                Object companyIdObj = userData.get("companyId");
                Object departmentIdObj = userData.get("departmentId");
                
                UUID companyId = companyIdObj != null ? UUID.fromString(companyIdObj.toString()) : null;
                UUID departmentId = departmentIdObj != null ? UUID.fromString(departmentIdObj.toString()) : null;
                
                if (companyId != null && departmentId != null) {
                    return Optional.of(new UserInfo(companyId, departmentId));
                }
            }
        } catch (Exception e) {
            logger.debug("Erro ao buscar informações do usuário {}: {}", userId, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    private Optional<UserInfo> getUserInfoByUsername(String username, String authToken) {
        try {
            // Busca usuários por email/username e pega o primeiro resultado
            String url = userServiceUrl + "/api/users?email=" + username + "&page=0&size=1";
            HttpHeaders headers = new HttpHeaders();
            if (authToken != null) {
                headers.set("Authorization", "Bearer " + authToken);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object contentObj = responseBody.get("content");
                
                if (contentObj instanceof java.util.List && !((java.util.List<?>) contentObj).isEmpty()) {
                    Object firstItem = ((java.util.List<?>) contentObj).get(0);
                    if (firstItem instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userData = (Map<String, Object>) firstItem;
                        Object companyIdObj = userData.get("companyId");
                        Object departmentIdObj = userData.get("departmentId");
                        
                        UUID companyId = companyIdObj != null ? UUID.fromString(companyIdObj.toString()) : null;
                        UUID departmentId = departmentIdObj != null ? UUID.fromString(departmentIdObj.toString()) : null;
                        
                        if (companyId != null && departmentId != null) {
                            return Optional.of(new UserInfo(companyId, departmentId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Erro ao buscar usuário por username {}: {}", username, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public record UserInfo(UUID companyId, UUID departmentId) {}
}
