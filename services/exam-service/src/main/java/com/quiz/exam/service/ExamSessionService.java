package com.quiz.exam.service;

import com.quiz.exam.entity.Exam;
import com.quiz.exam.exception.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExamSessionService {

    private static final String PREFIX = "exam:session:";

    private final StringRedisTemplate redisTemplate;

    public ExamSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void startAttempt(String userId, String examId, Exam exam) {
        Instant now = Instant.now();
        assertExamWindowOpen(exam, now);
        Instant deadline = ExamDeadlineCalculator.deadlineForAttempt(exam, now);
        String key = key(userId, examId);
        long ttlSeconds = Math.max(60, Duration.between(now, deadline).getSeconds() + 120);
        redisTemplate.opsForValue().set(key, String.valueOf(now.toEpochMilli()), Duration.ofSeconds(ttlSeconds));
    }

    public void assertSubmitAllowed(String userId, String examId, Exam exam) {
        Instant now = Instant.now();
        assertExamWindowOpen(exam, now);
        String key = key(userId, examId);
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Start the exam before submitting (session expired or missing)");
        }
        Instant startedAt = Instant.ofEpochMilli(Long.parseLong(raw));
        Instant deadline = ExamDeadlineCalculator.deadlineForAttempt(exam, startedAt);
        if (now.isAfter(deadline)) {
            throw new BadRequestException("Submission deadline has passed");
        }
    }

    public void completeAttempt(String userId, String examId) {
        redisTemplate.delete(key(userId, examId));
    }

    private static void assertExamWindowOpen(Exam exam, Instant now) {
        if (exam.getAvailableFrom() != null && now.isBefore(exam.getAvailableFrom())) {
            throw new BadRequestException("Exam is not open yet");
        }
        if (exam.getAvailableUntil() != null && now.isAfter(exam.getAvailableUntil())) {
            throw new BadRequestException("Exam window has closed");
        }
    }

    private static String key(String userId, String examId) {
        return PREFIX + userId + ":" + examId;
    }
}
