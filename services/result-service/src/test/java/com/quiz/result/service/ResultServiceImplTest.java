package com.quiz.result.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quiz.result.dto.BaseResponse;
import com.quiz.result.dto.ExamPolicyDto;
import com.quiz.result.dto.ExamResultResponse;
import com.quiz.result.dto.QuestionAnswerResponse;
import com.quiz.result.dto.SubmitAnswerRequest;
import com.quiz.result.entity.ExamResult;
import com.quiz.result.event.ExamSubmittedEventPublisher;
import com.quiz.result.exception.BadRequestException;
import com.quiz.result.exception.ConflictException;
import com.quiz.result.repository.ExamResultRepository;
import com.quiz.result.repository.ExamViolationEventRepository;
import com.quiz.result.repository.ExamViolationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResultServiceImplTest {

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private ExamViolationRepository examViolationRepository;

    @Mock
    private ExamViolationEventRepository examViolationEventRepository;

    @Mock
    private QuestionAnswerGateway questionAnswerGateway;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ExamSubmittedEventPublisher eventPublisher;

    @Mock
    private ExamServiceGateway examServiceGateway;

    @InjectMocks
    private ResultServiceImpl resultService;

    private SubmitAnswerRequest request;

    @BeforeEach
    void setUp() {
        request = new SubmitAnswerRequest(
                "11111111-1111-1111-1111-111111111111",
                Map.of("22222222-2222-2222-2222-222222222222", "A")
        );
        ReflectionTestUtils.setField(resultService, "internalServiceToken", "test-internal-token");
    }

    @Test
    void submitExam_shouldPublishEvent_whenSubmissionSuccess() {
        when(idempotencyService.acquireSubmissionKey("user-1", request.getExamId(), "idem-1")).thenReturn(true);
        when(examViolationRepository.findByUserIdAndExamId("user-1", request.getExamId()))
                .thenReturn(Optional.empty());
        when(examServiceGateway.getPolicy(eq(request.getExamId()), anyString()))
                .thenReturn(new BaseResponse<>(
                        Instant.now(),
                        200,
                        "ok",
                        new ExamPolicyDto(
                                request.getExamId(),
                                "Sample exam",
                                "PUBLISHED",
                                "teacher-1",
                                null,
                                null,
                                60,
                                3,
                                false,
                                true
                        )
                ));
        when(examResultRepository.countByUserIdAndExamId("user-1", request.getExamId())).thenReturn(0L);
        when(questionAnswerGateway.fetchCorrectAnswers(request.getExamId()))
                .thenReturn(List.of(new QuestionAnswerResponse("22222222-2222-2222-2222-222222222222", "A")));

        ExamResult saved = new ExamResult();
        saved.setId(10L);
        saved.setUserId("user-1");
        saved.setExamId(request.getExamId());
        saved.setScore(1);
        saved.setTotalQuestions(1);
        saved.setViolationCount(0);
        saved.setSubmittedAt(Instant.now());
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(saved);

        ExamResultResponse response = resultService.submitExam(request, "user-1", "user-1@example.com", "idem-1");

        assertEquals(1, response.getScore());
        verify(eventPublisher).publishExamSubmitted(any(), any(), any(), any(), any(), any(), any(), any());
        verify(examServiceGateway).validateSession(eq(request.getExamId()), eq("user-1"), anyString());
        verify(examServiceGateway).completeSession(eq(request.getExamId()), eq("user-1"), anyString());
    }

    @Test
    void submitExam_shouldReject_whenDuplicateIdempotencyKey() {
        when(idempotencyService.acquireSubmissionKey("user-1", request.getExamId(), "idem-1")).thenReturn(false);

        assertThrows(ConflictException.class, () -> resultService.submitExam(request, "user-1", "user-1@example.com", "idem-1"));

        verify(examResultRepository, never()).save(any());
        verify(eventPublisher, never()).publishExamSubmitted(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitExam_shouldReject_whenAnswerKeyUnavailable() {
        when(idempotencyService.acquireSubmissionKey("user-1", request.getExamId(), "idem-2")).thenReturn(true);
        when(examServiceGateway.getPolicy(eq(request.getExamId()), anyString()))
                .thenReturn(new BaseResponse<>(
                        Instant.now(),
                        200,
                        "ok",
                        new ExamPolicyDto(
                                request.getExamId(),
                                "Sample exam",
                                "PUBLISHED",
                                "teacher-1",
                                null,
                                null,
                                60,
                                3,
                                false,
                                true
                        )
                ));
        when(examResultRepository.countByUserIdAndExamId("user-1", request.getExamId())).thenReturn(0L);
        when(questionAnswerGateway.fetchCorrectAnswers(request.getExamId())).thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> resultService.submitExam(request, "user-1", "user-1@example.com", "idem-2")
        );

        verify(examResultRepository, never()).save(any());
        verify(eventPublisher, never()).publishExamSubmitted(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
