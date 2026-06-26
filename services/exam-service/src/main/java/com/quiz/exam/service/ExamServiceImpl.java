package com.quiz.exam.service;

import com.quiz.exam.dto.AttachQuestionRequest;
import com.quiz.exam.dto.AttachQuestionsBulkRequest;
import com.quiz.exam.dto.BaseResponse;
import com.quiz.exam.dto.ClassServiceClassResponse;
import com.quiz.exam.dto.ClassServiceStudentResponse;
import com.quiz.exam.dto.CreateExamRequest;
import com.quiz.exam.dto.ExamPolicyDto;
import com.quiz.exam.dto.ExamQuestionResponse;
import com.quiz.exam.dto.ExamResponse;
import com.quiz.exam.dto.ExamStartResponse;
import com.quiz.exam.dto.QuestionServiceBaseResponse;
import com.quiz.exam.dto.QuestionServiceQuestionResponse;
import com.quiz.exam.dto.UpdateExamRequest;
import com.quiz.exam.entity.Exam;
import com.quiz.exam.entity.ExamQuestion;
import com.quiz.exam.entity.ExamQuestionId;
import com.quiz.exam.entity.ExamStatus;
import com.quiz.exam.event.ExamCreatedEventPublisher;
import com.quiz.exam.exception.BadRequestException;
import com.quiz.exam.exception.ResourceNotFoundException;
import com.quiz.exam.repository.ExamQuestionRepository;
import com.quiz.exam.repository.ExamRepository;
import feign.FeignException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamServiceImpl implements ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamServiceImpl.class);

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionServiceGateway questionServiceGateway;
    private final ClassServiceGateway classServiceGateway;
    private final ExamCreatedEventPublisher examCreatedEventPublisher;
    private final ExamSessionService examSessionService;

    public ExamServiceImpl(
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            QuestionServiceGateway questionServiceGateway,
            ClassServiceGateway classServiceGateway,
            ExamCreatedEventPublisher examCreatedEventPublisher,
            ExamSessionService examSessionService
    ) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionServiceGateway = questionServiceGateway;
        this.classServiceGateway = classServiceGateway;
        this.examCreatedEventPublisher = examCreatedEventPublisher;
        this.examSessionService = examSessionService;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", allEntries = true)
    })
    public ExamResponse createExam(CreateExamRequest request, String createdBy) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setDuration(request.getDuration());
        exam.setClassId(request.getClassId());
        exam.setCreatedBy(createdBy);
        exam.setStatus(parseStatus(request.getStatus()));
        exam.setAvailableFrom(request.getAvailableFrom());
        exam.setAvailableUntil(request.getAvailableUntil());
        exam.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1);
        exam.setShowCorrectAnswers(request.getShowCorrectAnswers() != null ? request.getShowCorrectAnswers() : false);
        exam.setShowScoreImmediately(
                request.getShowScoreImmediately() != null ? request.getShowScoreImmediately() : true
        );
        normalizeAndValidateSchedule(exam, true);
        Exam saved = examRepository.save(exam);

        if (saved.getStatus() == ExamStatus.PUBLISHED) {
            notifyExamCreated(saved, createdBy);
        }
        return toResponse(saved, getExamQuestionIds(saved.getId()));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", allEntries = true)
    })
    public ExamResponse publishExam(String examId, String instructorId) {
        Exam exam = findExam(examId);
        assertInstructor(exam, instructorId);
        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            return toResponse(exam, getExamQuestionIds(examId));
        }
        List<String> questionIds = getExamQuestionIds(examId);
        if (questionIds.isEmpty()) {
            throw new BadRequestException("Exam has no questions yet");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        Exam saved = examRepository.save(exam);
        notifyExamCreated(saved, instructorId);
        return toResponse(saved, questionIds);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", allEntries = true)
    })
    public ExamResponse updateExam(String examId, UpdateExamRequest request, String instructorId) {
        Exam exam = findExam(examId);
        assertInstructor(exam, instructorId);
        if (request.getTitle() != null) {
            exam.setTitle(request.getTitle());
        }
        if (request.getDuration() != null) {
            exam.setDuration(request.getDuration());
        }
        if (request.getAvailableFrom() != null) {
            exam.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableUntil() != null) {
            exam.setAvailableUntil(request.getAvailableUntil());
        }
        if (request.getMaxAttempts() != null) {
            exam.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getShowCorrectAnswers() != null) {
            exam.setShowCorrectAnswers(request.getShowCorrectAnswers());
        }
        if (request.getShowScoreImmediately() != null) {
            exam.setShowScoreImmediately(request.getShowScoreImmediately());
        }
        normalizeAndValidateSchedule(exam, false);
        Exam saved = examRepository.save(exam);
        return toResponse(saved, getExamQuestionIds(examId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", allEntries = true)
    })
    public void deleteExam(String examId, String instructorId) {
        Exam exam = findExam(examId);
        assertInstructor(exam, instructorId);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new BadRequestException("Only draft exams can be deleted");
        }
        examQuestionRepository.deleteByIdExamId(examId);
        examRepository.delete(exam);
    }

    @Override
    @Cacheable(cacheNames = "exams")
    public List<ExamResponse> getAllExams() {
        return new ArrayList<>(examRepository.findAll().stream()
                .map(exam -> toResponse(exam, getExamQuestionIds(exam.getId())))
            .toList());
    }

    @Override
    public List<ExamResponse> getExamsByUserClasses(String userId) {
        BaseResponse<List<ClassServiceClassResponse>> response = classServiceGateway.getClassesByUserId(userId);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return List.of();
        }

        List<String> classIds = response.getData().stream()
                .map(ClassServiceClassResponse::getId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (classIds.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        return examRepository.findByClassIdIn(classIds).stream()
                .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                .filter(e -> isExamVisibleForStudentNow(e, now))
                .map(exam -> toResponse(exam, getExamQuestionIds(exam.getId())))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "exam-by-id", key = "#examId")
    public ExamResponse getExamById(String examId, String viewerUserId, String viewerRole) {
        Exam exam = findExam(examId);
        if (exam.getStatus() == ExamStatus.DRAFT) {
            boolean owner = viewerUserId != null && viewerUserId.equals(exam.getCreatedBy());
            boolean privileged = viewerRole != null
                    && (viewerRole.equals("ADMIN") || viewerRole.equals("INSTRUCTOR"));
            if (!owner && !privileged) {
                throw new ResourceNotFoundException("Exam not found: " + examId);
            }
        }
        return toResponse(exam, getExamQuestionIds(examId));
    }

    @Override
    @Transactional
    public ExamStartResponse startExam(String examId, String userId) {
        Exam exam = findExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new BadRequestException("Exam is not published");
        }
        List<String> questionIds = getExamQuestionIds(examId);
        if (questionIds.isEmpty()) {
            throw new BadRequestException("Exam has no questions yet");
        }
        Instant now = Instant.now();
        if (exam.getAvailableFrom() != null && now.isBefore(exam.getAvailableFrom())) {
            throw new BadRequestException("Exam is not open yet");
        }
        if (exam.getAvailableUntil() != null && now.isAfter(exam.getAvailableUntil())) {
            throw new BadRequestException("Exam window has closed");
        }
        examSessionService.startAttempt(userId, examId, exam);
        Instant deadline = ExamDeadlineCalculator.deadlineForAttempt(exam, now);
        ExamResponse body = toResponse(exam, questionIds);
        return ExamStartResponse.builder()
                .exam(body)
                .serverTime(now)
                .deadlineAt(deadline)
                .build();
    }

    @Override
    public ExamPolicyDto getExamPolicyForResult(String examId) {
        Exam exam = findExam(examId);
        return new ExamPolicyDto(
                exam.getId(),
                exam.getTitle(),
                exam.getStatus().name(),
                exam.getCreatedBy(),
                exam.getAvailableFrom(),
                exam.getAvailableUntil(),
                exam.getDuration(),
                exam.getMaxAttempts(),
                Boolean.TRUE.equals(exam.getShowCorrectAnswers()),
                Boolean.TRUE.equals(exam.getShowScoreImmediately())
        );
    }

    @Override
    public void assertSubmitSessionValid(String examId, String userId) {
        Exam exam = findExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new BadRequestException("Exam is not published");
        }
        examSessionService.assertSubmitAllowed(userId, examId, exam);
    }

    @Override
    public void completeExamSession(String examId, String userId) {
        findExam(examId);
        examSessionService.completeAttempt(userId, examId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", key = "#examId")
    })
    public void attachQuestion(String examId, AttachQuestionRequest request) {
        findExam(examId);
        validateQuestionExists(request.getQuestionId());
        ExamQuestionId examQuestionId = new ExamQuestionId(examId, request.getQuestionId());
        if (!examQuestionRepository.existsById(examQuestionId)) {
            examQuestionRepository.save(new ExamQuestion(examQuestionId));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "exams", allEntries = true),
            @CacheEvict(cacheNames = "exam-by-id", key = "#examId")
    })
    public void attachQuestionsBulk(String examId, AttachQuestionsBulkRequest request) {
        findExam(examId);
        if (request == null || request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            return;
        }

        List<String> distinctQuestionIds = new ArrayList<>(new HashSet<>(request.getQuestionIds()));

        List<ExamQuestion> toSave = new ArrayList<>();
        for (String questionId : distinctQuestionIds) {
            if (questionId == null || questionId.isBlank()) {
                continue;
            }

            validateQuestionExists(questionId);

            ExamQuestionId examQuestionId = new ExamQuestionId(examId, questionId);
            if (!examQuestionRepository.existsById(examQuestionId)) {
                toSave.add(new ExamQuestion(examQuestionId));
            }
        }

        if (!toSave.isEmpty()) {
            examQuestionRepository.saveAll(toSave);
        }
    }

    @Override
    public List<String> getExamQuestionIds(String examId) {
        findExam(examId);
        List<String> questionIds = examQuestionRepository.findByIdExamId(examId).stream()
                .map(mapping -> mapping.getId().getQuestionId())
                .toList();

        if (!questionIds.isEmpty()) {
            return questionIds;
        }

        QuestionServiceBaseResponse<List<QuestionServiceQuestionResponse>> response =
                questionServiceGateway.getQuestionsByExamId(examId);
        if (response == null || response.getData() == null) {
            return List.of();
        }

        return response.getData().stream()
                .map(QuestionServiceQuestionResponse::getId)
                .toList();
    }

    @Override
    public List<ExamQuestionResponse> getExamQuestions(String examId) {
        findExam(examId);
        List<String> questionIds = getExamQuestionIds(examId);

        List<ExamQuestionResponse> randomizedQuestions = questionIds.stream()
                .map(questionId -> {
                    QuestionServiceBaseResponse<QuestionServiceQuestionResponse> response =
                            questionServiceGateway.getQuestionById(questionId);
                    QuestionServiceQuestionResponse data = response.getData();
                    if (data == null) {
                        throw new ResourceNotFoundException("Question not found: " + questionId);
                    }
                    return toExamQuestionResponse(data);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(randomizedQuestions);
        return randomizedQuestions;
    }

    private static boolean isExamVisibleForStudentNow(Exam exam, Instant now) {
        if (exam.getAvailableFrom() != null && now.isBefore(exam.getAvailableFrom())) {
            return false;
        }
        if (exam.getAvailableUntil() != null && now.isAfter(exam.getAvailableUntil())) {
            return false;
        }
        return true;
    }

    private static void normalizeAndValidateSchedule(Exam exam, boolean validateFutureStart) {
        Instant from = exam.getAvailableFrom();
        Instant until = exam.getAvailableUntil();

        if (from != null && until != null) {
            if (!until.isAfter(from)) {
                throw new BadRequestException("availableUntil must be after availableFrom");
            }
            long diffMinutes = Duration.between(from, until).toMinutes();
            if (diffMinutes <= 0) {
                throw new BadRequestException("Exam duration must be positive");
            }
            if (diffMinutes > Integer.MAX_VALUE) {
                throw new BadRequestException("Exam window is too large");
            }
            // Keep backend as source of truth: if both timestamps are provided, duration follows window.
            exam.setDuration((int) diffMinutes);
        }

        if (exam.getDuration() == null || exam.getDuration() <= 0) {
            throw new BadRequestException("duration must be greater than 0");
        }

        if (validateFutureStart && from != null && from.isBefore(Instant.now())) {
            throw new BadRequestException("availableFrom must not be in the past");
        }
    }

    private static ExamStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return ExamStatus.DRAFT;
        }
        return ExamStatus.valueOf(raw.trim().toUpperCase());
    }

    private static void assertInstructor(Exam exam, String instructorId) {
        if (!exam.getCreatedBy().equals(instructorId)) {
            throw new BadRequestException("Only the exam owner can perform this action");
        }
    }

    private void notifyExamCreated(Exam saved, String createdBy) {
        List<String> notifyUserIds = resolveStudentUserIdsForNotifications(saved.getClassId(), createdBy);
        examCreatedEventPublisher.publish(
                saved.getId(),
                saved.getClassId(),
                saved.getTitle(),
                createdBy,
                notifyUserIds
        );
    }

    private List<String> resolveStudentUserIdsForNotifications(String classId, String createdBy) {
        if (classId == null || classId.isBlank()) {
            return List.of();
        }
        try {
            BaseResponse<List<ClassServiceStudentResponse>> response =
                    classServiceGateway.getStudentsByClassId(classId);
            if (response == null || response.getData() == null) {
                return List.of();
            }
            return response.getData().stream()
                    .map(ClassServiceStudentResponse::getUserId)
                    .filter(Objects::nonNull)
                    .filter(uid -> !uid.isBlank())
                    .filter(uid -> !uid.equals(createdBy))
                    .distinct()
                    .toList();
        } catch (FeignException ex) {
            log.warn("[NOTIFY] Could not load class students for exam notification (classId={}): {}",
                    classId, ex.getMessage());
            return List.of();
        }
    }

    private Exam findExam(String examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
    }

    private ExamResponse toResponse(Exam exam, List<String> questionIds) {
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .duration(exam.getDuration())
                .classId(exam.getClassId())
                .createdBy(exam.getCreatedBy())
                .createdAt(exam.getCreatedAt())
                .status(exam.getStatus().name())
                .availableFrom(exam.getAvailableFrom())
                .availableUntil(exam.getAvailableUntil())
                .maxAttempts(exam.getMaxAttempts())
                .showCorrectAnswers(exam.getShowCorrectAnswers())
                .showScoreImmediately(exam.getShowScoreImmediately())
                .questionIds(questionIds)
                .build();
    }

    private void validateQuestionExists(String questionId) {
        try {
            questionServiceGateway.getQuestionById(questionId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Question not found: " + questionId);
        }
    }

    private ExamQuestionResponse toExamQuestionResponse(QuestionServiceQuestionResponse question) {
        List<String> optionValues = List.of(
            question.getOptionA(),
            question.getOptionB(),
            question.getOptionC(),
            question.getOptionD()
        );
        List<String> optionLetters = List.of("A", "B", "C", "D");

        List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(order);

        List<String> shuffledOptions = new ArrayList<>(4);
        List<String> shuffledOptionKeys = new ArrayList<>(4);
        for (Integer idx : order) {
            shuffledOptions.add(optionValues.get(idx));
            shuffledOptionKeys.add(optionLetters.get(idx));
        }

        return new ExamQuestionResponse(
            question.getId(),
            question.getContent(),
            shuffledOptions,
            shuffledOptionKeys
        );
    }
}
