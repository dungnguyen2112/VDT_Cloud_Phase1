package com.quiz.notification.listener;

import com.quiz.notification.event.EmailVerificationRequestedEvent;
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
public class EmailVerificationRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationRequestedListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-email}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailVerificationRequestedListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "${app.rabbitmq.auth-user-registered-queue}")
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("[EVENT] Received auth.user.registered eventId={}, userId={}, email={}",
                event.getEventId(), event.getUserId(), event.getEmail());

        if (event.getEmail() == null || event.getEmail().isBlank()) {
            log.warn("[MAIL] Skip sending verification email: missing email (userId={})", event.getUserId());
            return;
        }

        try {
            sendVerificationMail(event);
            log.info("[MAIL] Sent verification email to {} (userId={})", event.getEmail(), event.getUserId());
        } catch (Exception ex) {
            log.error("[MAIL] Failed to send verification email (to={}, userId={})", event.getEmail(), event.getUserId(), ex);
        }
    }

    private void sendVerificationMail(EmailVerificationRequestedEvent event) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

        helper.setFrom(fromEmail, fromName);
        helper.setTo(event.getEmail());
        helper.setSubject("E-Mid Quiz - Xác thực email");

        String body = """
                Xin chào,

                Mã xác thực email của bạn là: %s

                Mã có hiệu lực trong 10 phút.

                Nếu bạn không yêu cầu đăng ký, vui lòng bỏ qua email này.

                Trân trọng,
                %s
                """.formatted(event.getVerificationCode(), fromName);

        helper.setText(body, false);
        mailSender.send(message);
    }
}

