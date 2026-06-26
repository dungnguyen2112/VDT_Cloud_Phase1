package com.quiz.notification.listener;

import com.quiz.notification.event.UserAddedToClassEvent;
import com.quiz.notification.service.NotificationPersistenceService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class UserAddedToClassListener {

    private static final Logger log = LoggerFactory.getLogger(UserAddedToClassListener.class);

    private final JavaMailSender mailSender;
    private final NotificationPersistenceService notificationPersistenceService;

    @Value("${app.mail.from-email}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public UserAddedToClassListener(
            JavaMailSender mailSender,
            NotificationPersistenceService notificationPersistenceService
    ) {
        this.mailSender = mailSender;
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @RabbitListener(queues = "${app.rabbitmq.class-user-added-queue}")
    public void handleUserAddedToClass(UserAddedToClassEvent event) {
        log.info("[EVENT] Received class.user.added eventId={}, userId={}, classId={}",
                event.getEventId(), event.getUserId(), event.getClassId());

        String title = "Bạn đã được thêm vào lớp";
        String body = "Lớp: " + (event.getClassName() != null && !event.getClassName().isBlank()
                ? event.getClassName()
                : "lớp học của bạn");
        notificationPersistenceService.saveInApp(event.getUserId(), "CLASS_USER_ADDED", title, body);

        if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            log.warn("[MAIL] Skip class invite email: missing userEmail (userId={}, classId={})",
                    event.getUserId(), event.getClassId());
            return;
        }

        try {
            sendMail(event);
            log.info("[MAIL] Sent class invite email to {} (classId={})", event.getUserEmail(), event.getClassId());
        } catch (Exception ex) {
            log.error("[MAIL] Failed to send class invite email (to={}, classId={})",
                    event.getUserEmail(), event.getClassId(), ex);
        }
    }

    private void sendMail(UserAddedToClassEvent event) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail, fromName);
        helper.setTo(event.getUserEmail());
        helper.setSubject("Bạn đã được thêm vào lớp: " + event.getClassName());
        helper.setText(
                "Xin chào,\n\nBạn đã được thêm vào lớp \"" + event.getClassName() + "\".\n\n"
                        + "Đăng nhập vào hệ thống để xem chi tiết.\n\n"
                        + "Trân trọng,\n" + fromName,
                false
        );
        mailSender.send(message);
    }
}
