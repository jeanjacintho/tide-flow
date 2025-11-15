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
}

