package com.quiz.result.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyService(
            StringRedisTemplate redisTemplate,
            @Value("${app.idempotency.ttl-minutes:15}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public boolean acquireSubmissionKey(String userId, String examId, String idempotencyKey) {
        String redisKey = "idem:submit:" + userId + ":" + examId + ":" + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }
}
