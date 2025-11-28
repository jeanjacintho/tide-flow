package br.jeanjacintho.tideflow.user_service.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;
    private final String fromEmail;
    private final boolean emailEnabled;
    private final boolean devMode;
    private final String devEmail;

    @Autowired
    public EmailService(
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${resend.from.email:onboarding@resend.dev}") String fromEmail,
            @Value("${resend.enabled:true}") boolean emailEnabled,
            @Value("${resend.dev-mode:false}") String devModeStr,
            @Value("${resend.dev-email:}") String devEmail) {
        this.fromEmail = fromEmail;
        this.emailEnabled = emailEnabled;
        this.devMode = devModeStr != null && !devModeStr.isEmpty() && Boolean.parseBoolean(devModeStr);
        this.devEmail = devEmail != null && !devEmail.isEmpty() ? devEmail : null;

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("RESEND_API_KEY não configurada. Emails não serão enviados.");
            this.resend = null;
        } else {
            this.resend = new Resend(apiKey);
        }

        if (devMode) {
            if (devEmail == null || devEmail.isEmpty()) {
                logger.warn("Modo dev ativado mas EMAIL_DEVMODE não configurado. Emails não serão redirecionados.");
            } else {
                logger.info("Modo de desenvolvimento ativado. Todos os emails serão redirecionados para: {}", devEmail);
            }
        }
    }

    EmailService(Resend resend, String fromEmail, boolean emailEnabled, boolean devMode, String devEmail) {
        this.resend = resend;
        this.fromEmail = fromEmail;
        this.emailEnabled = emailEnabled;
        this.devMode = devMode;
        this.devEmail = devEmail;
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

        String recipientEmail = to;
        if (devMode && devEmail != null && !devEmail.isEmpty()) {
            logger.info("Modo dev ativo: redirecionando email de {} para {}", to, devEmail);
            recipientEmail = devEmail;
        }

        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(recipientEmail)
                    .subject("Bem-vindo ao Tide Flow!")
                    .html(buildWelcomeEmailHtml(name))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            if (devMode) {
                logger.info("Email de boas-vindas enviado com sucesso para {} (redirecionado de {}) (ID: {})",
                    devEmail, to, response.getId());
            } else {
                logger.info("Email de boas-vindas enviado com sucesso para: {} (ID: {})", to, response.getId());
            }
        } catch (ResendException e) {
            logger.error("Erro ao enviar email de boas-vindas para: {}", recipientEmail, e);
        }
    }

    public void sendInvitationEmail(String to, String name, String email, String temporaryPassword, String companyName) {
        if (!emailEnabled) {
            logger.info("Email desabilitado. Email de convite para: {} (Login: {}, Senha: {})", to, email, temporaryPassword);
            return;
        }

        if (resend == null) {
            logger.warn("Resend não configurado. Email de convite não enviado para: {}", to);
            return;
        }

        String recipientEmail = to;
        if (devMode && devEmail != null && !devEmail.isEmpty()) {
            logger.info("Modo dev ativo: redirecionando email de {} para {}", to, devEmail);
            recipientEmail = devEmail;
        }

        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(recipientEmail)
                    .subject("Bem-vindo ao Tide Flow - Suas credenciais de acesso")
                    .html(buildInvitationEmailHtml(name, email, temporaryPassword, companyName))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            if (devMode) {
                logger.info("Email de convite enviado com sucesso para {} (redirecionado de {}) (ID: {})",
                    devEmail, to, response.getId());
            } else {
                logger.info("Email de convite enviado com sucesso para: {} (ID: {})", to, response.getId());
            }
        } catch (ResendException e) {
            logger.error("Erro ao enviar email de convite para: {}", recipientEmail, e);
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

    private String buildInvitationEmailHtml(String name, String email, String temporaryPassword, String companyName) {
        String userName = name != null && !name.isEmpty() ? name : "Usuário";
        String company = companyName != null && !companyName.isEmpty() ? companyName : "sua empresa";

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
            ".credentials { background-color: #fff; border: 2px solid #4CAF50; border-radius: 5px; padding: 15px; margin: 20px 0; }" +
            ".credential-item { margin: 10px 0; }" +
            ".credential-label { font-weight: bold; color: #4CAF50; }" +
            ".credential-value { font-family: monospace; font-size: 14px; background-color: #f5f5f5; padding: 5px 10px; border-radius: 3px; }" +
            ".warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }" +
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
            "<p>Sua conta foi criada pela <strong>%s</strong> no sistema Tide Flow.</p>" +
            "<p>Abaixo estão suas credenciais de acesso:</p>" +
            "<div class=\"credentials\">" +
            "<div class=\"credential-item\">" +
            "<span class=\"credential-label\">Email:</span><br>" +
            "<span class=\"credential-value\">%s</span>" +
            "</div>" +
            "<div class=\"credential-item\">" +
            "<span class=\"credential-label\">Senha Temporária:</span><br>" +
            "<span class=\"credential-value\">%s</span>" +
            "</div>" +
            "</div>" +
            "<div class=\"warning\">" +
            "<p><strong>⚠️ Importante:</strong> Por segurança, você precisará alterar sua senha no primeiro acesso.</p>" +
            "</div>" +
            "<p>Para acessar o sistema, utilize as credenciais acima. Após o primeiro login, você será solicitado a criar uma nova senha.</p>" +
            "<p>Se tiver alguma dúvida, entre em contato com o administrador da sua empresa.</p>" +
            "<p>Atenciosamente,<br>Equipe Tide Flow</p>" +
            "</div>" +
            "<div class=\"footer\">" +
            "<p>&copy; 2024 Tide Flow. Todos os direitos reservados.</p>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>",
            userName, company, email, temporaryPassword
        );
    }
}
