package br.jeanjacintho.tideflow.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final boolean emailEnabled;

    public EmailService(JavaMailSender mailSender, 
                       @Value("${spring.mail.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
    }

    public void sendWelcomeEmail(String to, String name) {
        if (!emailEnabled) {
            logger.info("Email desabilitado. Mensagem de boas-vindas para {}: {}", to, buildWelcomeMessage(name));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Bem-vindo ao Tide Flow!");
            message.setText(buildWelcomeMessage(name));
            
            mailSender.send(message);
            logger.info("Email de boas-vindas enviado com sucesso para: {}", to);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de boas-vindas para: {}", to, e);
            logger.warn("Continuando sem enviar email. Configure as credenciais de email para habilitar o envio.");
        }
    }

    private String buildWelcomeMessage(String name) {
        return String.format(
            "Olá %s,\n\n" +
            "Bem-vindo ao Tide Flow! Estamos muito felizes em tê-lo conosco.\n\n" +
            "Sua conta foi criada com sucesso e você já pode começar a usar nossa plataforma.\n\n" +
            "Se tiver alguma dúvida, não hesite em entrar em contato conosco.\n\n" +
            "Atenciosamente,\n" +
            "Equipe Tide Flow",
            name != null && !name.isEmpty() ? name : "Usuário"
        );
    }

    public void sendRiskAlert(String trustedEmail, String userName, String message, String riskLevel, String reason, String context) {
        if (!emailEnabled) {
            logger.info("Email desabilitado. Alerta de risco para {}: {}", trustedEmail, buildRiskAlertMessage(userName, message, riskLevel, reason, context));
            return;
        }

        if (trustedEmail == null || trustedEmail.isEmpty()) {
            logger.warn("Email de confiança não configurado. Alerta de risco não enviado para usuário: {}", userName);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(trustedEmail);
            mailMessage.setSubject("🚨 ALERTA VERMELHO - Tide Flow - " + userName + " precisa de atenção");
            mailMessage.setText(buildRiskAlertMessage(userName, message, riskLevel, reason, context));
            
            mailSender.send(mailMessage);
            logger.info("Alerta de risco enviado com sucesso para: {}", trustedEmail);
        } catch (Exception e) {
            logger.error("Erro ao enviar alerta de risco para: {}", trustedEmail, e);
        }
    }

    private String buildRiskAlertMessage(String userName, String message, String riskLevel, String reason, String context) {
        StringBuilder alertMessage = new StringBuilder();
        alertMessage.append("🚨 ALERTA VERMELHO - Sistema de Detecção de Risco\n\n");
        alertMessage.append("Olá,\n\n");
        alertMessage.append("O sistema Tide Flow detectou um possível risco de autolesão ou suicídio na conta de ");
        alertMessage.append(userName != null && !userName.isEmpty() ? userName : "um usuário");
        alertMessage.append(".\n\n");
        
        alertMessage.append("NÍVEL DE RISCO: ").append(riskLevel).append("\n\n");
        
        if (reason != null && !reason.isEmpty()) {
            alertMessage.append("Motivo da detecção: ").append(reason).append("\n\n");
        }
        
        if (message != null && !message.isEmpty()) {
            alertMessage.append("Mensagem detectada:\n");
            alertMessage.append("\"").append(message).append("\"\n\n");
        }
        
        if (context != null && !context.isEmpty()) {
            alertMessage.append("Contexto: ").append(context).append("\n\n");
        }
        
        alertMessage.append("AÇÃO RECOMENDADA:\n");
        alertMessage.append("- Entre em contato com ").append(userName != null ? userName : "o usuário").append(" o quanto antes\n");
        alertMessage.append("- Ofereça suporte emocional e escuta ativa\n");
        alertMessage.append("- Se necessário, busque ajuda profissional (CVV: 188, CAPS, etc.)\n");
        alertMessage.append("- Não ignore este alerta\n\n");
        
        alertMessage.append("Este é um alerta automático do sistema Tide Flow.\n");
        alertMessage.append("O sistema analisa mensagens usando inteligência artificial para detectar possíveis situações de risco.\n\n");
        
        alertMessage.append("Atenciosamente,\n");
        alertMessage.append("Sistema Tide Flow");
        
        return alertMessage.toString();
    }
}

