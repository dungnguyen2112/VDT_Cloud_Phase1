package com.quiz.notification.listener;

import com.quiz.notification.event.ExamCreatedEvent;
import com.quiz.notification.service.NotificationPersistenceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExamCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(ExamCreatedListener.class);

    private final NotificationPersistenceService notificationPersistenceService;

    public ExamCreatedListener(NotificationPersistenceService notificationPersistenceService) {
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @RabbitListener(queues = "${app.rabbitmq.exam-created-queue}")
    public void handleExamCreated(ExamCreatedEvent event) {
        log.info("[EVENT] Received exam.created eventId={}, examId={}, classId={}, recipients={}",
                event.getEventId(), event.getExamId(), event.getClassId(),
                event.getNotifyUserIds() != null ? event.getNotifyUserIds().size() : 0);

        List<String> ids = event.getNotifyUserIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String title = "Bài thi mới trong lớp";
        String body = "Có bài thi mới: \"" + event.getTitle() + "\".";

        for (String userId : ids) {
            if (userId == null || userId.isBlank()) {
                continue;
            }
            notificationPersistenceService.saveInApp(userId, "EXAM_CREATED", title, body);
        }
    }
}
