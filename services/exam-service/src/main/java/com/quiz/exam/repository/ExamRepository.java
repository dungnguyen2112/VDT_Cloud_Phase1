package com.quiz.exam.repository;

import com.quiz.exam.entity.Exam;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, String> {

	List<Exam> findByClassIdIn(Collection<String> classIds);
}
