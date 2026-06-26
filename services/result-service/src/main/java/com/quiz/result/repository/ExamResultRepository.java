package com.quiz.result.repository;

import com.quiz.result.entity.ExamResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByUserIdOrderBySubmittedAtDesc(String userId);

    boolean existsByUserIdAndExamId(String userId, String examId);

    long countByUserIdAndExamId(String userId, String examId);

    List<ExamResult> findByExamIdOrderBySubmittedAtDesc(String examId);

    Optional<ExamResult> findTopByUserIdAndExamIdOrderBySubmittedAtDesc(String userId, String examId);
}

