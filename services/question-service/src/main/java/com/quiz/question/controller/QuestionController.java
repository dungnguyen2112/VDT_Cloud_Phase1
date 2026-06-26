package com.quiz.question.controller;

import com.quiz.question.dto.BaseResponse;
import com.quiz.question.dto.CreateQuestionRequest;
import com.quiz.question.dto.GenerateQuestionRequest;
import com.quiz.question.dto.QuestionAnswerResponse;
import com.quiz.question.dto.QuestionResponse;
import com.quiz.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@Tag(name = "Question", description = "Question management endpoints")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Create question")
    public ResponseEntity<BaseResponse<QuestionResponse>> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse data = questionService.createQuestion(request);
        BaseResponse<QuestionResponse> response = BaseResponse.of(HttpStatus.CREATED.value(), "Question created", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all questions")
    public ResponseEntity<BaseResponse<List<QuestionResponse>>> getAllQuestions() {
        List<QuestionResponse> data = questionService.getAllQuestions();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Questions retrieved", data));
    }

    @GetMapping("/bank")
    @Operation(summary = "Get question bank")
    public ResponseEntity<BaseResponse<List<QuestionResponse>>> getQuestionBank(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category) {
        List<QuestionResponse> data = questionService.getQuestionBank(keyword, category);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question bank retrieved", data));
    }

    @GetMapping("/bank/categories")
    @Operation(summary = "Get question categories")
    public ResponseEntity<BaseResponse<List<String>>> getQuestionCategories() {
        List<String> data = questionService.getQuestionCategories();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question categories retrieved", data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get question by ID")
    public ResponseEntity<BaseResponse<QuestionResponse>> getQuestionById(@PathVariable String id) {
        QuestionResponse data = questionService.getQuestionById(id);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question retrieved", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Update question")
    public ResponseEntity<BaseResponse<QuestionResponse>> updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse data = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question updated", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Delete question")
    public ResponseEntity<BaseResponse<Void>> deleteQuestion(@PathVariable String id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question deleted", null));
    }

    @GetMapping("/exam/{examId}")
    @Operation(summary = "Get questions by exam ID")
    public ResponseEntity<BaseResponse<List<QuestionResponse>>> getQuestionsByExamId(@PathVariable String examId) {
        List<QuestionResponse> data = questionService.getQuestionsByExamId(examId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Questions retrieved", data));
    }

    @GetMapping("/exam/{examId}/answers")
    @Operation(summary = "Get answers by exam ID")
    public ResponseEntity<BaseResponse<List<QuestionAnswerResponse>>> getAnswersByExamId(@PathVariable String examId) {
        List<QuestionAnswerResponse> data = questionService.getAnswersByExamId(examId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Answers retrieved", data));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Import questions from CSV/Excel")
    @ApiResponse(responseCode = "201", description = "Questions imported")
    public ResponseEntity<BaseResponse<List<QuestionResponse>>> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "examId", required = false) String examId
    ) {
        List<QuestionResponse> data = questionService.importQuestions(file, examId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED.value(), "Questions imported", data));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Generate basic AI questions")
    @ApiResponse(responseCode = "201", description = "Questions generated")
    public ResponseEntity<BaseResponse<List<QuestionResponse>>> generateQuestions(
            @Valid @RequestBody GenerateQuestionRequest request,
            @RequestParam(name = "examId", required = false) String examId
    ) {
        List<QuestionResponse> data = questionService.generateQuestions(request, examId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED.value(), "Questions generated", data));
    }
}
