package br.jeanjacintho.tideflow.user_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import br.jeanjacintho.tideflow.user_service.config.RabbitMQConfig;
import br.jeanjacintho.tideflow.user_service.event.UserCreatedEvent;

@Service
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserCreated(UserCreatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_CREATED_QUEUE, event);
    }
}
