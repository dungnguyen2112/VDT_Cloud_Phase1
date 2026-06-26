package com.quiz.question.repository;

import com.quiz.question.entity.Question;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByContentContainingIgnoreCase(String keyword);
    List<Question> findByCategoryIgnoreCase(String category);
    List<Question> findByCategoryIgnoreCaseAndContentContainingIgnoreCase(String category, String keyword);

    @Query("select distinct q.category from Question q where q.category is not null and q.category <> '' order by q.category asc")
    List<String> findDistinctCategories();

    @Query(value = "SELECT q.* FROM questions q " +
            "INNER JOIN exam_questions eq ON q.id = eq.question_id " +
            "WHERE eq.exam_id = :examId", nativeQuery = true)
    List<Question> findByExamId(@Param("examId") String examId);

    @Modifying
    @Query(value = "INSERT IGNORE INTO exam_questions(exam_id, question_id) VALUES (:examId, :questionId)", nativeQuery = true)
    void linkQuestionToExam(@Param("examId") String examId, @Param("questionId") String questionId);
}
