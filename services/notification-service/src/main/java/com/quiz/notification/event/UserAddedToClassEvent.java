package com.quiz.notification.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class UserAddedToClassEvent {
    String eventId;
    String eventType;
    Instant occurredAt;
    String userId;
    String userEmail;
    String classId;
    String className;
}
