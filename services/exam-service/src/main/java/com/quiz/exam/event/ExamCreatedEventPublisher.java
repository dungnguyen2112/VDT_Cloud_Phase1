package com.quiz.exam.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExamCreatedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExamCreatedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public ExamCreatedEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.exam-created-routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(
            String examId,
            String classId,
            String title,
            String createdBy,
            List<String> notifyUserIds
    ) {
        ExamCreatedEvent event = new ExamCreatedEvent(
                UUID.randomUUID().toString(),
                "exam.created",
                Instant.now(),
                examId,
                classId,
                title,
                createdBy,
                notifyUserIds
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("[EVENT] Published exam.created eventId={}, examId={}, classId={}, recipients={}",
                event.getEventId(), examId, classId, notifyUserIds != null ? notifyUserIds.size() : 0);
    }
}
