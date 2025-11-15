package br.jeanjacintho.tideflow.user_service.service;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import br.jeanjacintho.tideflow.user_service.config.RabbitMQConfig;
import br.jeanjacintho.tideflow.user_service.event.UserCreatedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventPublisher Tests")
class UserEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserEventPublisher eventPublisher;

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
    @DisplayName("publishUserCreated - Deve publicar evento na fila correta")
    void testPublishUserCreated() {
        eventPublisher.publishUserCreated(testEvent);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.USER_CREATED_QUEUE),
            eq(testEvent)
        );
    }
}

