package br.jeanjacintho.tideflow.user_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private Resend resend;

    @Mock
    private Emails emails;

    @Mock
    private CreateEmailResponse emailResponse;

    private EmailService emailService;
    private String testEmail;
    private String testName;

    @BeforeEach
    void setUp() {
        testEmail = "john@example.com";
        testName = "John Doe";
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve enviar email quando habilitado e Resend configurado")
    void testSendWelcomeEmailWhenEnabled() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(emailResponse);
        when(emailResponse.getId()).thenReturn("email-id-123");

        emailService = new EmailService(resend, "onboarding@resend.dev", true, false, "dev@example.com");
        emailService.sendWelcomeEmail(testEmail, testName);

        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Não deve enviar email quando desabilitado")
    void testSendWelcomeEmailWhenDisabled() throws ResendException {
        emailService = new EmailService(resend, "onboarding@resend.dev", false, false, "dev@example.com");
        emailService.sendWelcomeEmail(testEmail, testName);

        verify(emails, org.mockito.Mockito.never()).send(any(CreateEmailOptions.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Não deve enviar email quando Resend é null")
    void testSendWelcomeEmailWhenResendIsNull() throws ResendException {
        Resend nullResend = null;
        emailService = new EmailService(nullResend, "onboarding@resend.dev", true, false, "dev@example.com");
        emailService.sendWelcomeEmail(testEmail, testName);

        verify(emails, org.mockito.Mockito.never()).send(any(CreateEmailOptions.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve tratar erro de envio graciosamente")
    void testSendWelcomeEmailHandlesError() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        doThrow(ResendException.class).when(emails).send(any(CreateEmailOptions.class));

        emailService = new EmailService(resend, "onboarding@resend.dev", true, false, "dev@example.com");
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(testEmail, testName));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve usar 'Usuário' quando nome for null")
    void testSendWelcomeEmailWithNullName() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(emailResponse);
        when(emailResponse.getId()).thenReturn("email-id-123");

        emailService = new EmailService(resend, "onboarding@resend.dev", true, false, "dev@example.com");
        emailService.sendWelcomeEmail(testEmail, null);

        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve usar 'Usuário' quando nome for vazio")
    void testSendWelcomeEmailWithEmptyName() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(emailResponse);
        when(emailResponse.getId()).thenReturn("email-id-123");

        emailService = new EmailService(resend, "onboarding@resend.dev", true, false, "dev@example.com");
        emailService.sendWelcomeEmail(testEmail, "");

        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    @DisplayName("sendWelcomeEmail - Deve redirecionar email para dev email quando em modo dev")
    void testSendWelcomeEmailInDevMode() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(emailResponse);
        when(emailResponse.getId()).thenReturn("email-id-123");

        String devEmail = "dev@example.com";
        emailService = new EmailService(resend, "onboarding@resend.dev", true, true, devEmail);
        emailService.sendWelcomeEmail(testEmail, testName);

        ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emails).send(captor.capture());

        CreateEmailOptions capturedOptions = captor.getValue();
        assertEquals(1, capturedOptions.getTo().size());
        assertEquals(devEmail, capturedOptions.getTo().get(0));
    }
}
