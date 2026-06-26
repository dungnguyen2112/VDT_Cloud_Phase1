package com.quiz.exam.event;

import java.time.Instant;
import java.util.List;
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
public class ExamCreatedEvent {
    String eventId;
    String eventType;
    Instant occurredAt;
    String examId;
    String classId;
    String title;
    String createdBy;
    List<String> notifyUserIds;
}
