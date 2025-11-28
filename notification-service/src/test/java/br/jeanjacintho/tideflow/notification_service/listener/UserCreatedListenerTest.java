package br.jeanjacintho.tideflow.notification_service.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.jeanjacintho.tideflow.notification_service.dto.UserCreatedEvent;
import br.jeanjacintho.tideflow.notification_service.service.EmailService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCreatedListener Tests")
class UserCreatedListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserCreatedListener listener;

    private UserCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new UserCreatedEvent(
            UUID.randomUUID(),
            "John Doe",
            "john@example.com"
        );
    }

    @Test
    @DisplayName("handleUserCreated - Deve chamar EmailService com dados corretos")
    void testHandleUserCreated() {
        listener.handleUserCreated(testEvent);

        verify(emailService).sendWelcomeEmail(
            eq(testEvent.email()),
            eq(testEvent.name())
        );
    }
}
