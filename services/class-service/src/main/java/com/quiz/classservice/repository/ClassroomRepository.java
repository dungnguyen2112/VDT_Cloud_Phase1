package com.quiz.classservice.repository;

import com.quiz.classservice.entity.Classroom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, String> {

    Optional<Classroom> findByJoinCodeIgnoreCase(String joinCode);

    boolean existsByJoinCodeIgnoreCase(String joinCode);
}
