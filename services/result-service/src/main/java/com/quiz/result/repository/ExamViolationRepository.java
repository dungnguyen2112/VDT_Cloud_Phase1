package com.quiz.result.repository;

import com.quiz.result.entity.ExamViolation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamViolationRepository extends JpaRepository<ExamViolation, Long> {

    Optional<ExamViolation> findByUserIdAndExamId(String userId, String examId);
}
