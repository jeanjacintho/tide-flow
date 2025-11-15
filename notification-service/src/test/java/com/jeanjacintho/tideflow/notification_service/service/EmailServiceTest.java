package com.jeanjacintho.tideflow.notification_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private String testEmail;
    private String testName;

    @BeforeEach
    void setUp() {
        testEmail = "john@example.com";
        testName = "John Doe";
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve enviar email quando habilitado")
    void testSendWelcomeEmailWhenEnabled() {
        EmailService enabledService = new EmailService(mailSender, true);

        enabledService.sendWelcomeEmail(testEmail, testName);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Não deve enviar email quando desabilitado")
    void testSendWelcomeEmailWhenDisabled() {
        EmailService disabledService = new EmailService(mailSender, false);

        disabledService.sendWelcomeEmail(testEmail, testName);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve tratar erro de envio graciosamente")
    void testSendWelcomeEmailHandlesError() {
        EmailService enabledService = new EmailService(mailSender, true);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> enabledService.sendWelcomeEmail(testEmail, testName));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve configurar mensagem corretamente")
    void testSendWelcomeEmailMessageConfiguration() {
        EmailService enabledService = new EmailService(mailSender, true);

        enabledService.sendWelcomeEmail(testEmail, testName);

        verify(mailSender).send(argThat((SimpleMailMessage message) -> 
            message.getTo() != null &&
            message.getTo()[0].equals(testEmail) &&
            message.getSubject().equals("Bem-vindo ao Tide Flow!") &&
            message.getText().contains(testName)
        ));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve usar 'Usuário' quando nome for null")
    void testSendWelcomeEmailWithNullName() {
        EmailService enabledService = new EmailService(mailSender, true);

        enabledService.sendWelcomeEmail(testEmail, null);

        verify(mailSender).send(argThat((SimpleMailMessage message) -> 
            message.getText().contains("Usuário")
        ));
    }
}

