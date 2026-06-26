package com.quiz.question.service;

import com.quiz.question.dto.CreateQuestionRequest;
import com.quiz.question.dto.GenerateQuestionRequest;
import com.quiz.question.dto.QuestionAnswerResponse;
import com.quiz.question.dto.QuestionResponse;
import com.quiz.question.entity.Question;
import com.quiz.question.exception.ResourceNotFoundException;
import com.quiz.question.mapper.QuestionMapper;
import com.quiz.question.repository.QuestionRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class QuestionServiceImpl implements QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionServiceImpl.class);
    private static final String DEFAULT_CATEGORY = "GENERAL";

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final GeminiQuestionGenerator geminiQuestionGenerator;
    private final OpenAiQuestionGenerator openAiQuestionGenerator;

    public QuestionServiceImpl(
            QuestionRepository questionRepository,
            QuestionMapper questionMapper,
            GeminiQuestionGenerator geminiQuestionGenerator,
            OpenAiQuestionGenerator openAiQuestionGenerator
    ) {
        this.questionRepository = questionRepository;
        this.questionMapper = questionMapper;
        this.geminiQuestionGenerator = geminiQuestionGenerator;
        this.openAiQuestionGenerator = openAiQuestionGenerator;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "questions", allEntries = true),
        @CacheEvict(cacheNames = "questions", allEntries = true)
    })
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctAnswer(request.getCorrectAnswer())
                .category(normalizeCategory(request.getCategory()))
                .build();
        Question saved = questionRepository.save(question);
        return questionMapper.toResponse(saved);
    }

    @Override
    @Cacheable(cacheNames = "questions")
    public List<QuestionResponse> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    @Override
    public List<QuestionResponse> getQuestionBank(String keyword, String category) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;

        List<Question> questions;
        if (StringUtils.hasText(normalizedCategory) && StringUtils.hasText(normalizedKeyword)) {
            questions = questionRepository.findByCategoryIgnoreCaseAndContentContainingIgnoreCase(
                    normalizedCategory,
                    normalizedKeyword
            );
        } else if (StringUtils.hasText(normalizedCategory)) {
            questions = questionRepository.findByCategoryIgnoreCase(normalizedCategory);
        } else if (StringUtils.hasText(normalizedKeyword)) {
            questions = questionRepository.findByContentContainingIgnoreCase(normalizedKeyword);
        } else {
            questions = questionRepository.findAll();
        }

        // Newest questions first for search/category browsing in question bank.
        return questions.stream()
                .sorted(Comparator.comparing(Question::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(questionMapper::toResponse)
                .toList();
    }

    @Override
    public List<String> getQuestionCategories() {
        return questionRepository.findDistinctCategories();
    }

    @Override
    @Cacheable(cacheNames = "question-by-id", key = "#questionId")
    public QuestionResponse getQuestionById(String questionId) {
        Question question = findQuestion(questionId);
        return questionMapper.toResponse(question);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "questions", allEntries = true),
        @CacheEvict(cacheNames = "question-by-id", key = "#questionId")
    })
    public QuestionResponse updateQuestion(String questionId, CreateQuestionRequest request) {
        Question existing = findQuestion(questionId);
        existing.setContent(request.getContent());
        existing.setOptionA(request.getOptionA());
        existing.setOptionB(request.getOptionB());
        existing.setOptionC(request.getOptionC());
        existing.setOptionD(request.getOptionD());
        existing.setCorrectAnswer(request.getCorrectAnswer());
        existing.setCategory(normalizeCategory(request.getCategory()));
        Question saved = questionRepository.save(existing);
        return questionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "questions", allEntries = true),
        @CacheEvict(cacheNames = "question-by-id", key = "#questionId")
    })
    public void deleteQuestion(String questionId) {
        Question existing = findQuestion(questionId);
        questionRepository.delete(existing);
    }

    @Override
    public List<QuestionResponse> getQuestionsByExamId(String examId) {
        List<Question> questions = questionRepository.findByExamId(examId);
        return questions.stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    @Override
    public List<QuestionAnswerResponse> getAnswersByExamId(String examId) {
        List<Question> questions = questionRepository.findByExamId(examId);
        return questions.stream()
                .map(questionMapper::toAnswerResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<QuestionResponse> importQuestions(MultipartFile file, String examId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<Question> imported = fileName.endsWith(".xlsx")
                ? parseExcel(file)
                : parseCsv(file);

        List<QuestionResponse> responses = imported.stream()
                .map(questionRepository::save)
                .peek(saved -> linkIfNeeded(examId, saved.getId()))
                .map(questionMapper::toResponse)
                .toList();

        log.info("[EVENT] Imported {} questions for examId={}", responses.size(), examId);
        return responses;
    }

    @Override
    @Transactional
    public List<QuestionResponse> generateQuestions(GenerateQuestionRequest request, String examId) {
        String topic = request.getTopic();
        int expected = request.getCount();
        String category = normalizeCategory(request.getCategory());

        // Gemini first, then merge OpenAI batch for any shortfall (keys missing, timeout, bad JSON, etc.).
        List<Question> candidates = new ArrayList<>(geminiQuestionGenerator.generate(topic, expected));
        int geminiCount = candidates.size();
        if (candidates.size() < expected) {
            log.warn(
                    "[EVENT] Gemini returned {}/{} questions; merging OpenAI batch.",
                    candidates.size(),
                    expected
            );
            for (Question q : openAiQuestionGenerator.generate(topic, expected)) {
                if (candidates.size() >= expected) {
                    break;
                }
                candidates.add(q);
            }
        }

        int afterMerge = candidates.size();
        if (candidates.size() < expected) {
            throw new IllegalArgumentException(
                    "AI did not return enough valid questions for this topic. expected=" + expected
                            + ", fromGemini=" + geminiCount
                            + ", totalAfterOpenAiMerge=" + afterMerge
                            + ". Nothing was saved. Check API keys, AI_PROVIDER, model names, timeouts, and logs on question-service "
                            + "(Gemini/OpenAI HTTP status and response body). The model must return a JSON array only, each item with "
                            + "content, optionA–D, correctAnswer A|B|C|D."
            );
        }

        if (candidates.size() > expected) {
            candidates = new ArrayList<>(candidates.subList(0, expected));
        }

        List<QuestionResponse> generated = new ArrayList<>();
        for (Question question : candidates) {
            question.setCategory(category);
            Question saved = questionRepository.save(question);
            linkIfNeeded(examId, saved.getId());
            generated.add(questionMapper.toResponse(saved));
        }
        log.info("[EVENT] Generated {} questions for topic={}, examId={}",
                generated.size(), topic, examId);
        return generated;
    }

    private Question findQuestion(String questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
    }

    private List<Question> parseCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(true).setHeader().build().parse(reader)) {
            List<Question> questions = new ArrayList<>();
            for (CSVRecord row : parser) {
                questions.add(buildQuestion(
                        row.get("content"),
                        row.get("optionA"),
                        row.get("optionB"),
                        row.get("optionC"),
                        row.get("optionD"),
                        row.get("correctAnswer")
                ));
            }
            return questions;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot read CSV file", ex);
        }
    }

    private List<Question> parseExcel(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Question> questions = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                questions.add(buildQuestion(
                        readCell(row, 0),
                        readCell(row, 1),
                        readCell(row, 2),
                        readCell(row, 3),
                        readCell(row, 4),
                        readCell(row, 5)
                ));
            }
            return questions;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot read Excel file", ex);
        }
    }

    private String readCell(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : cell.toString().trim();
    }

    private Question buildQuestion(String content, String optionA, String optionB, String optionC, String optionD, String correctAnswer) {
        String normalized = correctAnswer == null ? "" : correctAnswer.trim().toUpperCase(Locale.ROOT);
        if (!List.of("A", "B", "C", "D").contains(normalized)) {
            throw new IllegalArgumentException("correctAnswer must be A, B, C or D");
        }
        return Question.builder()
                .content(content)
                .optionA(optionA)
                .optionB(optionB)
                .optionC(optionC)
                .optionD(optionD)
                .correctAnswer(normalized)
                .category(DEFAULT_CATEGORY)
                .build();
    }

    private static String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return DEFAULT_CATEGORY;
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return normalized;
    }

    private void linkIfNeeded(String examId, String questionId) {
        if (examId != null && !examId.isBlank()) {
            questionRepository.linkQuestionToExam(examId, questionId);
        }
    }
}
