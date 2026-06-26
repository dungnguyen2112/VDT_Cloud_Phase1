package com.quiz.notification.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.quiz.notification.event.ExamSubmittedEvent;
import com.quiz.notification.service.NotificationPersistenceService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;

class ExamSubmittedListenerTest {

    @Test
    void handleExamSubmitted_shouldProcessWithoutException() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        NotificationPersistenceService persistence = Mockito.mock(NotificationPersistenceService.class);
        ExamSubmittedListener listener = new ExamSubmittedListener(mailSender, persistence);
        ExamSubmittedEvent event = new ExamSubmittedEvent(
                "evt-1",
                "exam.submitted",
                Instant.now(),
                "user-1",
                "",
                "exam-1",
                "Kiểm tra giữa kỳ",
                99L,
                8,
                10,
                Instant.now().toString()
        );

        assertDoesNotThrow(() -> listener.handleExamSubmitted(event));
    }
}
