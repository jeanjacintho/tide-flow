package br.jeanjacintho.tideflow.notification_service.listener;

import br.jeanjacintho.tideflow.notification_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RiskAlertListener {

    private static final Logger logger = LoggerFactory.getLogger(RiskAlertListener.class);
    private final EmailService emailService;

    public RiskAlertListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "risk.alert")
    public void handleRiskAlert(Map<String, Object> eventMap) {
        logger.info("Alerta de risco recebido no notification-service: {}", eventMap.keySet());
        
        try {
            String userName = (String) eventMap.getOrDefault("userName", "Usuário");
            String trustedEmail = (String) eventMap.getOrDefault("trustedEmail", "");
            String message = (String) eventMap.getOrDefault("message", "");
            String riskLevel = (String) eventMap.getOrDefault("riskLevel", "UNKNOWN");
            String reason = (String) eventMap.getOrDefault("reason", "");
            String context = (String) eventMap.getOrDefault("context", "");

            logger.info("Processando alerta de risco - Usuário: {}, Email: {}, Nível: {}", userName, trustedEmail, riskLevel);

            if (trustedEmail != null && !trustedEmail.isEmpty()) {
                logger.info("Enviando email de alerta de risco para: {}", trustedEmail);
                emailService.sendRiskAlert(
                        trustedEmail,
                        userName,
                        message,
                        riskLevel,
                        reason,
                        context
                );
                logger.info("Email de alerta de risco processado com sucesso para: {}", trustedEmail);
            } else {
                logger.warn("Alerta de risco recebido mas email de confiança não configurado para usuário: {}. Evento completo: {}", userName, eventMap);
            }
        } catch (Exception e) {
            logger.error("Erro ao processar alerta de risco: {}", e.getMessage(), e);
            logger.error("Evento que causou o erro: {}", eventMap);
        }
    }
}
