package com.quiz.notification.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponse {
    String id;
    String type;
    String title;
    String body;
    String channel;
    Instant createdAt;
}
