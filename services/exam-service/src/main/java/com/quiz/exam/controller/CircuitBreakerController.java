package com.quiz.exam.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/circuit-breakers")
    public ResponseEntity<CircuitBreakerSnapshotResponse> getCircuitBreakerStates() {
        List<CircuitBreakerSnapshotItem> items = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .sorted(Comparator.comparing(CircuitBreaker::getName))
                .map(cb -> {
                    CircuitBreaker.Metrics metrics = cb.getMetrics();
                    return new CircuitBreakerSnapshotItem(
                            cb.getName(),
                            cb.getState().name(),
                            metrics.getFailureRate(),
                                                                                                                metrics.getSlowCallRate(),
                            metrics.getNumberOfBufferedCalls(),
                            metrics.getNumberOfFailedCalls(),
                            metrics.getNumberOfSlowCalls(),
                            metrics.getNumberOfNotPermittedCalls()
                    );
                })
                .toList();

        CircuitBreakerSnapshotResponse body = new CircuitBreakerSnapshotResponse(
                Instant.now(),
                HttpStatus.OK.value(),
                "Circuit breaker states retrieved",
                items
        );
        return ResponseEntity.ok(body);
    }

    public record CircuitBreakerSnapshotResponse(
            Instant timestamp,
            int status,
            String message,
            List<CircuitBreakerSnapshotItem> data
    ) {
    }

    public record CircuitBreakerSnapshotItem(
            String name,
            String state,
            float failureRate,
            float slowCallRate,
            int bufferedCalls,
            int failedCalls,
            int slowCalls,
            long notPermittedCalls
    ) {
    }
}
