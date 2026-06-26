package com.quiz.notification.service;

import com.quiz.notification.domain.Notification;
import com.quiz.notification.dto.NotificationResponse;
import com.quiz.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    public NotificationPersistenceService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void saveInApp(String userId, String type, String title, String body) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .channel("IN_APP")
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .channel(n.getChannel())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
