package com.quiz.exam.service;

import com.quiz.exam.entity.Exam;
import java.time.Instant;

public final class ExamDeadlineCalculator {

    private ExamDeadlineCalculator() {
    }

    /**
     * Latest submit time for an attempt that started at {@code startedAt}.
     */
    public static Instant deadlineForAttempt(Exam exam, Instant startedAt) {
        Instant byDuration = startedAt.plusSeconds(exam.getDuration() * 60L);
        if (exam.getAvailableUntil() == null) {
            return byDuration;
        }
        return byDuration.isBefore(exam.getAvailableUntil()) ? byDuration : exam.getAvailableUntil();
    }
}
