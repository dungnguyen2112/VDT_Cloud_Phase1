package com.quiz.classservice.service;

import com.quiz.classservice.dto.AddUserToClassRequest;
import com.quiz.classservice.dto.ClassResponse;
import com.quiz.classservice.dto.CreateClassRequest;
import com.quiz.classservice.dto.JoinClassRequest;
import com.quiz.classservice.dto.StudentResponse;
import java.util.List;

public interface ClassService {

    ClassResponse createClass(CreateClassRequest request, String teacherId);

    List<ClassResponse> getAllClasses();

    ClassResponse getClassById(String classId, String viewerUserId);

    ClassResponse joinClassByCode(JoinClassRequest request, String userId, String userEmail);

    ClassResponse regenerateJoinCode(String classId, String actorUserId, String actorRole);

    void addUserToClass(String classId, AddUserToClassRequest request, String actorUserId, String actorRole);

    void removeUserFromClass(String classId, String studentUserId, String actorUserId, String actorRole);

    List<StudentResponse> getStudentsByClassId(String classId, String actorUserId, String actorRole);

    List<ClassResponse> getClassesByUserId(String userId);
}
