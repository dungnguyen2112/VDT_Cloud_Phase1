package com.quiz.result.repository;

import com.quiz.result.entity.ExamViolationEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamViolationEventRepository extends JpaRepository<ExamViolationEvent, Long> {

    List<ExamViolationEvent> findTop200ByExamIdOrderByCreatedAtDesc(String examId);

    List<ExamViolationEvent> findTop200ByExamIdAndUserIdOrderByCreatedAtDesc(String examId, String userId);
}

