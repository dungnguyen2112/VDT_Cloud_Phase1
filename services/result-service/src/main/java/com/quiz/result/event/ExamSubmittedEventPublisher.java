package com.quiz.result.event;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExamSubmittedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExamSubmittedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public ExamSubmittedEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.exam-submitted-routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publishExamSubmitted(
            Long resultId,
            String userId,
            String userEmail,
            String examId,
            String examTitle,
            Integer score,
            Integer totalQuestions,
            Instant submittedAt
    ) {
        ExamSubmittedEvent event = new ExamSubmittedEvent(
                UUID.randomUUID().toString(),
                "exam.submitted",
                Instant.now(),
                userId,
                userEmail,
                examId,
                examTitle,
                resultId,
                score,
                totalQuestions,
                submittedAt.toString()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("[EVENT] Published exam.submitted eventId={}, userId={}, examId={}, resultId={}",
                event.getEventId(), userId, examId, resultId);
    }
}
