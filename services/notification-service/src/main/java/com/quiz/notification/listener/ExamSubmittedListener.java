package com.quiz.notification.listener;

import com.quiz.notification.event.ExamSubmittedEvent;
import com.quiz.notification.service.NotificationPersistenceService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Component
public class ExamSubmittedListener {

    private static final Logger log = LoggerFactory.getLogger(ExamSubmittedListener.class);

    private final JavaMailSender mailSender;
    private final NotificationPersistenceService notificationPersistenceService;

    @Value("${app.mail.from-email}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public ExamSubmittedListener(
            JavaMailSender mailSender,
            NotificationPersistenceService notificationPersistenceService
    ) {
        this.mailSender = mailSender;
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @RabbitListener(queues = "${app.rabbitmq.exam-submitted-queue}")
    public void handleExamSubmitted(ExamSubmittedEvent event) {
        log.info("[EVENT] Received exam.submitted eventId={}, userId={}, examId={}, score={}/{}",
                event.getEventId(), event.getUserId(), event.getExamId(), event.getScore(), event.getTotalQuestions());

        String inAppTitle = "Kết quả bài thi";
        String examLabel = event.getExamTitle() != null && !event.getExamTitle().isBlank()
                ? "\"" + event.getExamTitle() + "\""
                : "Bài thi";
        String inAppBody = examLabel + " — Điểm: " + event.getScore() + "/" + event.getTotalQuestions();
        notificationPersistenceService.saveInApp(event.getUserId(), "EXAM_SUBMITTED", inAppTitle, inAppBody);

        if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            log.warn("[MAIL] Skip sending email: missing userEmail (userId={}, examId={})",
                    event.getUserId(), event.getExamId());
            return;
        }

        try {
            sendExamSubmittedMail(event);
            log.info("[MAIL] Sent exam result email to {} (resultId={})", event.getUserEmail(), event.getResultId());
        } catch (Exception ex) {
            // Keep consumer running even if mail fails
            log.error("[MAIL] Failed to send email (to={}, resultId={})", event.getUserEmail(), event.getResultId(), ex);
        }
    }

    private void sendExamSubmittedMail(ExamSubmittedEvent event) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

        helper.setFrom(fromEmail, fromName);
        helper.setTo(event.getUserEmail());
        String subjectExam = event.getExamTitle() != null && !event.getExamTitle().isBlank()
                ? event.getExamTitle()
                : "bài thi";
        helper.setSubject("Kết quả bài thi: " + subjectExam);

        String body = """
                Xin chào,

                Bài thi của bạn đã được chấm điểm.

                Bài thi: %s
                Điểm: %d/%d

                Trân trọng,
                %s
                """.formatted(
                subjectExam,
                event.getScore(),
                event.getTotalQuestions(),
                fromName
        );

        helper.setText(body, false);
        mailSender.send(message);
    }
}
