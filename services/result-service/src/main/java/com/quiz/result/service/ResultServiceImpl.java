package com.quiz.result.service;

import com.quiz.result.dto.ExamPolicyDto;
import com.quiz.result.dto.ExamResultResponse;
import com.quiz.result.dto.QuestionAnswerResponse;
import com.quiz.result.dto.SubmitAnswerRequest;
import com.quiz.result.dto.ViolationEventResponse;
import com.quiz.result.dto.ViolationResponse;
import com.quiz.result.entity.ExamResult;
import com.quiz.result.entity.ExamViolation;
import com.quiz.result.entity.ExamViolationEvent;
import com.quiz.result.event.ExamSubmittedEventPublisher;
import com.quiz.result.exception.BadRequestException;
import com.quiz.result.exception.ConflictException;
import com.quiz.result.repository.ExamResultRepository;
import com.quiz.result.repository.ExamViolationEventRepository;
import com.quiz.result.repository.ExamViolationRepository;
import feign.FeignException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultServiceImpl implements ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultServiceImpl.class);

    private final ExamResultRepository examResultRepository;
    private final ExamViolationRepository examViolationRepository;
    private final ExamViolationEventRepository examViolationEventRepository;
    private final QuestionAnswerGateway questionAnswerGateway;
    private final IdempotencyService idempotencyService;
    private final ExamSubmittedEventPublisher eventPublisher;
    private final ExamServiceGateway examServiceGateway;

    private final String internalServiceToken;

    public ResultServiceImpl(
            ExamResultRepository examResultRepository,
            ExamViolationRepository examViolationRepository,
            ExamViolationEventRepository examViolationEventRepository,
            QuestionAnswerGateway questionAnswerGateway,
            IdempotencyService idempotencyService,
            ExamSubmittedEventPublisher eventPublisher,
            ExamServiceGateway examServiceGateway,
            @Value("${app.internal.service-token:}") String internalServiceToken
    ) {
        this.examResultRepository = examResultRepository;
        this.examViolationRepository = examViolationRepository;
        this.examViolationEventRepository = examViolationEventRepository;
        this.questionAnswerGateway = questionAnswerGateway;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.examServiceGateway = examServiceGateway;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    @Transactional
    public ExamResultResponse submitExam(SubmitAnswerRequest request, String userId, String userEmail, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ConflictException("Idempotency-Key header is required");
        }

        boolean acquired = idempotencyService.acquireSubmissionKey(userId, request.getExamId(), idempotencyKey);
        if (!acquired) {
            log.warn("[SECURITY] Duplicate idempotency key detected for userId={}, examId={}", userId, request.getExamId());
            throw new ConflictException("Duplicate submission request detected");
        }

        ExamPolicyDto policy = loadPolicy(request.getExamId());
        if (!"PUBLISHED".equals(policy.getStatus())) {
            throw new ConflictException("Exam is not available for submission");
        }

        long priorAttempts = examResultRepository.countByUserIdAndExamId(userId, request.getExamId());
        if (priorAttempts >= policy.getMaxAttempts()) {
            throw new ConflictException("Maximum attempts reached for this exam");
        }

        assertSessionValid(request.getExamId(), userId);

        Map<String, String> answers = request.getAnswers();
        List<QuestionAnswerResponse> correctAnswers = questionAnswerGateway.fetchCorrectAnswers(request.getExamId());

        if (correctAnswers == null || correctAnswers.isEmpty()) {
            throw new BadRequestException("Answer key unavailable for this exam. Please try again later.");
        }

        int totalQuestions = correctAnswers.size();
        int score = 0;

        for (QuestionAnswerResponse qa : correctAnswers) {
            String userAnswer = answers.get(qa.getQuestionId());
            String normalizedUserAnswer = normalizeAnswerLetter(userAnswer);
            String normalizedCorrectAnswer = normalizeAnswerLetter(qa.getCorrectAnswer());
            if (normalizedUserAnswer != null
                    && normalizedCorrectAnswer != null
                    && normalizedUserAnswer.equals(normalizedCorrectAnswer)) {
                score++;
            }
        }

        int violationCountAtSubmit = examViolationRepository.findByUserIdAndExamId(userId, request.getExamId())
                .map(v -> v.getViolationCount() == null ? 0 : v.getViolationCount())
                .orElse(0);

        ExamResult result = new ExamResult();
        result.setUserId(userId);
        result.setExamId(request.getExamId());
        result.setScore(score);
        result.setTotalQuestions(totalQuestions);
        result.setViolationCount(violationCountAtSubmit);

        ExamResult saved;
        try {
            saved = examResultRepository.save(result);
        } catch (DataIntegrityViolationException ex) {
            log.warn("[SECURITY] DB-level duplicate submit rejected for userId={}, examId={}", userId, request.getExamId());
            throw new ConflictException("Exam already submitted");
        }

        completeSession(request.getExamId(), userId);

        eventPublisher.publishExamSubmitted(
                saved.getId(),
                saved.getUserId(),
                userEmail,
                saved.getExamId(),
                policy.getTitle(),
                saved.getScore(),
                saved.getTotalQuestions(),
                saved.getSubmittedAt()
        );

        List<QuestionAnswerResponse> revealed =
                policy.isShowCorrectAnswers() ? correctAnswers : null;

        ExamResultResponse fullResponse = toResponse(saved, revealed);

        if (policy.isShowScoreImmediately()) {
            return fullResponse;
        }

        // Hide score/totalQuestions (and revealed answers) from the submit response
        // when teacher configured exam to not reveal score immediately.
        return new ExamResultResponse(
                fullResponse.getId(),
                fullResponse.getUserId(),
                fullResponse.getExamId(),
                null,
                null,
                fullResponse.getViolationCount(),
                fullResponse.getSubmittedAt(),
                null
        );
    }

    private ExamPolicyDto loadPolicy(String examId) {
        if (internalServiceToken == null || internalServiceToken.isBlank()) {
            throw new BadRequestException("INTERNAL_SERVICE_TOKEN is not configured on result-service");
        }
        try {
            var wrapper = examServiceGateway.getPolicy(examId, internalServiceToken);
            if (wrapper == null || wrapper.getData() == null) {
                throw new BadRequestException("Exam policy unavailable");
            }
            return wrapper.getData();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Exam not found");
        }
    }

    private void assertSessionValid(String examId, String userId) {
        try {
            examServiceGateway.validateSession(examId, userId, internalServiceToken);
        } catch (FeignException ex) {
            if (ex.status() >= 400 && ex.status() < 500) {
                throw new BadRequestException("Exam session invalid: start the exam before submitting");
            }
            throw ex;
        }
    }

    private void completeSession(String examId, String userId) {
        try {
            examServiceGateway.completeSession(examId, userId, internalServiceToken);
        } catch (FeignException ex) {
            log.warn("[EXAM] Session complete failed examId={} userId={}: {}", examId, userId, ex.getMessage());
        }
    }

    private static String normalizeAnswerLetter(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!List.of("A", "B", "C", "D").contains(normalized)) {
            return null;
        }
        return normalized;
    }

    @Override
    @Transactional
    public ViolationResponse reportViolation(String examId, String userId, String type) {
        String normalizedType = normalizeViolationType(type);

        ExamViolationEvent event = new ExamViolationEvent();
        event.setUserId(userId);
        event.setExamId(examId);
        event.setType(normalizedType);
        examViolationEventRepository.save(event);

        ExamViolation violation = examViolationRepository.findByUserIdAndExamId(userId, examId)
                .orElseGet(() -> {
                    ExamViolation created = new ExamViolation();
                    created.setUserId(userId);
                    created.setExamId(examId);
                    created.setViolationCount(0);
                    return created;
                });

        int nextCount = violation.getViolationCount() == null ? 1 : violation.getViolationCount() + 1;
        violation.setViolationCount(nextCount);
        examViolationRepository.save(violation);

        examResultRepository.findTopByUserIdAndExamIdOrderBySubmittedAtDesc(userId, examId)
                .ifPresent(result -> {
                    result.setViolationCount(nextCount);
                    examResultRepository.save(result);
                });

        log.warn("[SECURITY] Violation reported for userId={}, examId={}, type={}, count={}",
                userId, examId, normalizedType, nextCount);
        return new ViolationResponse(userId, examId, nextCount);
    }

    @Override
    public List<ViolationEventResponse> getViolationEventsForExam(String examId, String instructorUserId, String userId) {
        ExamPolicyDto policy = loadPolicy(examId);
        if (!policy.getCreatedBy().equals(instructorUserId)) {
            throw new AccessDeniedException("Only the exam owner can view violation events");
        }

        List<ExamViolationEvent> rows = (userId != null && !userId.isBlank())
                ? examViolationEventRepository.findTop200ByExamIdAndUserIdOrderByCreatedAtDesc(examId, userId.trim())
                : examViolationEventRepository.findTop200ByExamIdOrderByCreatedAtDesc(examId);

        return rows.stream()
                .map(e -> new ViolationEventResponse(e.getId(), e.getUserId(), e.getExamId(), e.getType(), e.getCreatedAt()))
                .toList();
    }

    private static String normalizeViolationType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "UNKNOWN";
        }
        String t = raw.trim().toUpperCase();
        return switch (t) {
            case "TAB_HIDDEN", "FULLSCREEN_EXIT" -> t;
            default -> "UNKNOWN";
        };
    }

    @Override
    public List<ExamResultResponse> getResultsByUserId(String userId) {
        return getResultsByUserId(userId, false);
    }

    @Override
    public List<ExamResultResponse> getResultsByUserId(String userId, boolean applyStudentVisibilityPolicy) {
        return examResultRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(r -> mapForResultsView(r, applyStudentVisibilityPolicy))
                .toList();
    }

    @Override
    public List<ExamResultResponse> getExamResultsForReport(String examId, String instructorUserId) {
        ExamPolicyDto policy = loadPolicy(examId);
        if (!policy.getCreatedBy().equals(instructorUserId)) {
            throw new AccessDeniedException("Only the exam owner can view this report");
        }
        return examResultRepository.findByExamIdOrderBySubmittedAtDesc(examId).stream()
                .map(r -> toResponse(r, null))
                .toList();
    }

    @Override
    public String exportExamResultsCsv(String examId, String instructorUserId) {
        List<ExamResultResponse> rows = getExamResultsForReport(examId, instructorUserId);
        StringBuilder sb = new StringBuilder();
        sb.append("resultId,userId,examId,score,totalQuestions,violationCount,submittedAt\n");
        for (ExamResultResponse r : rows) {
            sb.append(r.getId()).append(',')
                    .append(r.getUserId()).append(',')
                    .append(r.getExamId()).append(',')
                    .append(r.getScore()).append(',')
                    .append(r.getTotalQuestions()).append(',')
                    .append(r.getViolationCount()).append(',')
                    .append(r.getSubmittedAt()).append('\n');
        }
        return sb.toString();
    }

    private ExamResultResponse toResponse(ExamResult result, List<QuestionAnswerResponse> revealed) {
        return new ExamResultResponse(
                result.getId(),
                result.getUserId(),
                result.getExamId(),
                result.getScore(),
                result.getTotalQuestions(),
                result.getViolationCount(),
                result.getSubmittedAt(),
                revealed
        );
    }

    private ExamResultResponse mapForResultsView(ExamResult result, boolean applyStudentVisibilityPolicy) {
        ExamResultResponse full = toResponse(result, null);
        if (!applyStudentVisibilityPolicy) {
            return full;
        }

        try {
            ExamPolicyDto policy = loadPolicy(result.getExamId());
            if (policy.isShowScoreImmediately()) {
                List<QuestionAnswerResponse> revealed =
                        policy.isShowCorrectAnswers()
                                ? questionAnswerGateway.fetchCorrectAnswers(result.getExamId())
                                : null;
                return toResponse(result, revealed);
            }
        } catch (Exception ex) {
            log.warn("[VISIBILITY] Cannot load policy for examId={} while serving my-results. Hiding score by default.",
                    result.getExamId());
        }

        return new ExamResultResponse(
                full.getId(),
                full.getUserId(),
                full.getExamId(),
                null,
                null,
                full.getViolationCount(),
                full.getSubmittedAt(),
                null
        );
    }
}
