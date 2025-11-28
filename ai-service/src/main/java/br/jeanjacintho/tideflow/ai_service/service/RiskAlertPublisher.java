package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.RiskAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskAlertPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RiskAlertPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private static final String RISK_ALERT_QUEUE = "risk.alert";

    public RiskAlertPublisher(RabbitTemplate rabbitTemplate,
                              RestTemplate restTemplate,
                              @Value("${app.user-service.url:http://localhost:8080}") String userServiceUrl) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public void publishRiskAlert(String userId, String message, RiskAnalysisResponse riskAnalysis) {
        logger.info("Iniciando publicação de alerta de risco para usuário {}: nível={}, mensagem='{}'",
            userId, riskAnalysis.getRiskLevel(), message != null ? message.substring(0, Math.min(50, message.length())) : "null");

        try {
            logger.debug("Buscando dados do usuário {} do user-service", userId);
            Map<String, Object> userData = fetchUserData(userId);

            if (userData == null) {
                logger.warn("Não foi possível buscar dados do usuário {}. Alerta não será enviado.", userId);
                return;
            }

            logger.debug("Dados do usuário recebidos: {}", userData.keySet());

            String trustedEmail = (String) userData.get("trustedEmail");
            String userName = (String) userData.get("name");

            logger.info("Usuário {} - Nome: {}, Email de confiança: {}", userId, userName, trustedEmail != null ? "configurado" : "NÃO configurado");

            if (trustedEmail == null || trustedEmail.isEmpty()) {
                logger.warn("Usuário {} não possui email de confiança configurado. Alerta não será enviado. Dados do usuário: {}", userId, userData);
                return;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("userId", UUID.fromString(userId));
            event.put("userName", userName != null ? userName : "Usuário");
            event.put("trustedEmail", trustedEmail);
            event.put("message", message != null ? message : "");
            event.put("riskLevel", riskAnalysis.getRiskLevel() != null ? riskAnalysis.getRiskLevel() : "UNKNOWN");
            event.put("reason", riskAnalysis.getReason() != null ? riskAnalysis.getReason() : "");
            event.put("context", riskAnalysis.getContext() != null ? riskAnalysis.getContext() : "");

            logger.info("Publicando evento no RabbitMQ para fila {}: userId={}, trustedEmail={}, riskLevel={}",
                RISK_ALERT_QUEUE, userId, trustedEmail, riskAnalysis.getRiskLevel());

            rabbitTemplate.convertAndSend(RISK_ALERT_QUEUE, event);
            logger.info("Alerta de risco publicado com sucesso para usuário {} com nível {} - Email será enviado para: {}",
                userId, riskAnalysis.getRiskLevel(), trustedEmail);

        } catch (Exception e) {
            logger.error("Erro ao publicar alerta de risco para usuário {}: {}", userId, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserData(String userId) {
        try {
            String url = userServiceUrl + "/users/" + userId;
            logger.debug("Fazendo requisição GET para: {}", url);

            Map<String, Object> userData = restTemplate.getForObject(url, Map.class);

            if (userData == null) {
                logger.warn("Resposta do user-service foi null para usuário {}", userId);
                return null;
            }

            logger.debug("Dados do usuário recebidos com sucesso: {}", userData.keySet());
            return userData;

        } catch (RestClientException e) {
            logger.error("Erro ao buscar dados do usuário {} do user-service (URL: {}): {}",
                userId, userServiceUrl + "/users/" + userId, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Erro inesperado ao buscar dados do usuário {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }
}
