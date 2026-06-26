package com.quiz.exam.repository;

import com.quiz.exam.entity.ExamQuestion;
import com.quiz.exam.entity.ExamQuestionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestionId> {

    List<ExamQuestion> findByIdExamId(String examId);

    void deleteByIdExamId(String examId);
}
