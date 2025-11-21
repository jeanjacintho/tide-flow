package br.jeanjacintho.tideflow.user_service.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final Resend resend;
    private final String fromEmail;
    private final boolean emailEnabled;

    public EmailService(
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${resend.from.email:onboarding@resend.dev}") String fromEmail,
            @Value("${resend.enabled:true}") boolean emailEnabled) {
        this.fromEmail = fromEmail;
        this.emailEnabled = emailEnabled;
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("RESEND_API_KEY não configurada. Emails não serão enviados.");
            this.resend = null;
        } else {
            this.resend = new Resend(apiKey);
        }
    }

    EmailService(Resend resend, String fromEmail, boolean emailEnabled) {
        this.resend = resend;
        this.fromEmail = fromEmail;
        this.emailEnabled = emailEnabled;
    }

    public void sendWelcomeEmail(String to, String name) {
        if (!emailEnabled) {
            logger.info("Email desabilitado. Mensagem de boas-vindas para: {}", to);
            return;
        }

        if (resend == null) {
            logger.warn("Resend não configurado. Email de boas-vindas não enviado para: {}", to);
            return;
        }

        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject("Bem-vindo ao Tide Flow!")
                    .html(buildWelcomeEmailHtml(name))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            logger.info("Email de boas-vindas enviado com sucesso para: {} (ID: {})", to, response.getId());
        } catch (ResendException e) {
            logger.error("Erro ao enviar email de boas-vindas para: {}", to, e);
        }
    }

    private String buildWelcomeEmailHtml(String name) {
        String userName = name != null && !name.isEmpty() ? name : "Usuário";
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }" +
            ".content { padding: 20px; background-color: #f9f9f9; }" +
            ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<div class=\"header\">" +
            "<h1>Bem-vindo ao Tide Flow!</h1>" +
            "</div>" +
            "<div class=\"content\">" +
            "<p>Olá <strong>%s</strong>,</p>" +
            "<p>Estamos muito felizes em tê-lo conosco!</p>" +
            "<p>Sua conta foi criada com sucesso e você já pode começar a usar nossa plataforma.</p>" +
            "<p>Se tiver alguma dúvida, não hesite em entrar em contato conosco.</p>" +
            "<p>Atenciosamente,<br>Equipe Tide Flow</p>" +
            "</div>" +
            "<div class=\"footer\">" +
            "<p>&copy; 2024 Tide Flow. Todos os direitos reservados.</p>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>",
            userName
        );
    }
}

