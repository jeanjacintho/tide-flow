package br.jeanjacintho.tideflow.notification_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import br.jeanjacintho.tideflow.notification_service.dto.UserCreatedEvent;
import br.jeanjacintho.tideflow.notification_service.service.EmailService;

@Component
public class UserCreatedListener {

    private final EmailService emailService;

    public UserCreatedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "user.created")
    public void handleUserCreated(UserCreatedEvent event) {
        emailService.sendWelcomeEmail(event.email(), event.name());
    }
}
