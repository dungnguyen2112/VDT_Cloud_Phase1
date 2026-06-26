package com.quiz.classservice.repository;

import com.quiz.classservice.entity.UserClass;
import com.quiz.classservice.entity.UserClassId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserClassRepository extends JpaRepository<UserClass, UserClassId> {

    boolean existsByIdUserIdAndIdClassId(String userId, String classId);

    List<UserClass> findByIdClassId(String classId);

    List<UserClass> findByIdUserId(String userId);
}
