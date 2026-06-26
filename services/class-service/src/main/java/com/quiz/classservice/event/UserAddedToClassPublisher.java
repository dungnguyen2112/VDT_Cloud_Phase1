package com.quiz.classservice.event;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserAddedToClassPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserAddedToClassPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public UserAddedToClassPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.class-user-added-routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(String userId, String userEmail, String classId, String className) {
        UserAddedToClassEvent event = new UserAddedToClassEvent(
                UUID.randomUUID().toString(),
                "class.user.added",
                Instant.now(),
                userId,
                userEmail,
                classId,
                className
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("[EVENT] Published class.user.added eventId={}, userId={}, classId={}",
                event.getEventId(), userId, classId);
    }
}
