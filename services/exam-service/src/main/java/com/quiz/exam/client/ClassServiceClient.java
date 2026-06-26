package com.quiz.exam.client;

import com.quiz.exam.dto.BaseResponse;
import com.quiz.exam.dto.ClassServiceClassResponse;
import com.quiz.exam.dto.ClassServiceStudentResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "class-service")
public interface ClassServiceClient {

    @GetMapping("/api/v1/users/{userId}/classes")
    BaseResponse<List<ClassServiceClassResponse>> getClassesByUserId(@PathVariable("userId") String userId);

    @GetMapping("/api/v1/classes/{id}/students")
    BaseResponse<List<ClassServiceStudentResponse>> getStudentsByClassId(@PathVariable("id") String classId);
}
