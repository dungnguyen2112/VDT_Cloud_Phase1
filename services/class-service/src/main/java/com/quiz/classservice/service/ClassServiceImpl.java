package com.quiz.classservice.service;

import com.quiz.classservice.dto.AddUserToClassRequest;
import com.quiz.classservice.dto.ClassResponse;
import com.quiz.classservice.dto.CreateClassRequest;
import com.quiz.classservice.dto.JoinClassRequest;
import com.quiz.classservice.dto.StudentResponse;
import com.quiz.classservice.entity.Classroom;
import com.quiz.classservice.entity.UserClass;
import com.quiz.classservice.entity.UserClassId;
import com.quiz.classservice.exception.BadRequestException;
import com.quiz.classservice.exception.ConflictException;
import com.quiz.classservice.exception.ResourceNotFoundException;
import com.quiz.classservice.event.UserAddedToClassPublisher;
import com.quiz.classservice.repository.ClassroomRepository;
import com.quiz.classservice.repository.UserClassRepository;
import com.quiz.classservice.util.JoinCodeGenerator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassroomRepository classroomRepository;
    private final UserClassRepository userClassRepository;
    private final UserAddedToClassPublisher userAddedToClassPublisher;

    private final String joinUrlBase;

    public ClassServiceImpl(
            ClassroomRepository classroomRepository,
            UserClassRepository userClassRepository,
            UserAddedToClassPublisher userAddedToClassPublisher,
            @Value("${app.class.join-url-base:}") String joinUrlBase
    ) {
        this.classroomRepository = classroomRepository;
        this.userClassRepository = userClassRepository;
        this.userAddedToClassPublisher = userAddedToClassPublisher;
        this.joinUrlBase = joinUrlBase;
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, String teacherId) {
        Classroom classroom = new Classroom();
        classroom.setName(request.getName());
        classroom.setTeacherId(teacherId);
        classroom.setJoinCode(allocateUniqueJoinCode());
        Classroom saved = classroomRepository.save(classroom);
        return toResponse(saved, true);
    }

    @Override
    public List<ClassResponse> getAllClasses() {
        return classroomRepository.findAll().stream()
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Override
    @Transactional
    public ClassResponse getClassById(String classId, String viewerUserId) {
        Classroom classroom = findClassroom(classId);
        boolean showSecrets = viewerUserId != null && viewerUserId.equals(classroom.getTeacherId());
        if (showSecrets) {
            classroom = ensureJoinCode(classroom);
        }
        return toResponse(classroom, showSecrets);
    }

    @Override
    @Transactional
    public ClassResponse joinClassByCode(JoinClassRequest request, String userId, String userEmail) {
        String code = request.getJoinCode() != null ? request.getJoinCode().trim().toUpperCase() : "";
        if (code.isEmpty()) {
            throw new BadRequestException("Join code is required");
        }

        Classroom classroom = classroomRepository.findByJoinCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired join code"));

        if (userId.equals(classroom.getTeacherId())) {
            throw new BadRequestException("Instructors manage membership from the class dashboard; do not use the student join code");
        }

        if (userClassRepository.existsByIdUserIdAndIdClassId(userId, classroom.getId())) {
            throw new ConflictException("Already enrolled in this class");
        }

        userClassRepository.save(UserClass.builder()
                .id(new UserClassId(userId, classroom.getId()))
                .build());

        String email = userEmail != null ? userEmail : "";
        userAddedToClassPublisher.publish(userId, email, classroom.getId(), classroom.getName());

        return toResponse(classroom, false);
    }

    @Override
    @Transactional
    public ClassResponse regenerateJoinCode(String classId, String actorUserId, String actorRole) {
        Classroom classroom = findClassroom(classId);
        assertClassManager(classroom, actorUserId, actorRole);
        classroom.setJoinCode(allocateUniqueJoinCode());
        Classroom saved = classroomRepository.save(classroom);
        return toResponse(saved, true);
    }

    @Override
    @Transactional
    public void addUserToClass(String classId, AddUserToClassRequest request, String actorUserId, String actorRole) {
        Classroom classroom = findClassroom(classId);
        assertClassManager(classroom, actorUserId, actorRole);

        if (userClassRepository.existsByIdUserIdAndIdClassId(request.getUserId(), classId)) {
            return;
        }

        userClassRepository.save(UserClass.builder()
                .id(new UserClassId(request.getUserId(), classId))
                .build());

        String email = request.getUserEmail() != null ? request.getUserEmail() : "";
        userAddedToClassPublisher.publish(request.getUserId(), email, classId, classroom.getName());
    }

    @Override
    @Transactional
    public void removeUserFromClass(String classId, String studentUserId, String actorUserId, String actorRole) {
        Classroom classroom = findClassroom(classId);
        assertClassManager(classroom, actorUserId, actorRole);
        UserClassId id = new UserClassId(studentUserId, classId);
        if (userClassRepository.existsById(id)) {
            userClassRepository.deleteById(id);
        }
    }

    @Override
    public List<StudentResponse> getStudentsByClassId(String classId, String actorUserId, String actorRole) {
        Classroom classroom = findClassroom(classId);
        assertCanViewStudents(classroom, actorUserId, actorRole);
        return userClassRepository.findByIdClassId(classId).stream()
                .map(userClass -> StudentResponse.builder().userId(userClass.getId().getUserId()).build())
                .toList();
    }

    private void assertCanViewStudents(Classroom classroom, String actorUserId, String actorRole) {
        if ("ADMIN".equals(actorRole)) {
            return;
        }
        if (actorUserId != null && actorUserId.equals(classroom.getTeacherId())) {
            return;
        }
        // Students enrolled in this class can view member list in read-only mode.
        if ("STUDENT".equals(actorRole) && actorUserId != null
                && userClassRepository.existsByIdUserIdAndIdClassId(actorUserId, classroom.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view class students");
    }

    private static void assertClassManager(Classroom classroom, String actorUserId, String actorRole) {
        if ("ADMIN".equals(actorRole)) {
            return;
        }
        if (actorUserId != null && actorUserId.equals(classroom.getTeacherId())) {
            return;
        }
        throw new AccessDeniedException("Only the class teacher or an admin can access this resource");
    }

    @Override
    public List<ClassResponse> getClassesByUserId(String userId) {
        List<String> classIds = userClassRepository.findByIdUserId(userId).stream()
                .map(userClass -> userClass.getId().getClassId())
                .distinct()
                .toList();

        if (classIds.isEmpty()) {
            return List.of();
        }

        Map<String, Classroom> classesById = classroomRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity()));

        return classIds.stream()
                .map(classesById::get)
                .filter(java.util.Objects::nonNull)
                .map(c -> toResponse(c, false))
                .toList();
    }

    private String allocateUniqueJoinCode() {
        String code;
        do {
            code = JoinCodeGenerator.next();
        } while (classroomRepository.existsByJoinCodeIgnoreCase(code));
        return code;
    }

    private Classroom ensureJoinCode(Classroom classroom) {
        if (classroom.getJoinCode() != null && !classroom.getJoinCode().isBlank()) {
            return classroom;
        }
        classroom.setJoinCode(allocateUniqueJoinCode());
        return classroomRepository.save(classroom);
    }

    private Classroom findClassroom(String classId) {
        return classroomRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
    }

    private ClassResponse toResponse(Classroom classroom, boolean includeJoinSecrets) {
        if (!includeJoinSecrets || classroom.getJoinCode() == null || classroom.getJoinCode().isBlank()) {
            return ClassResponse.builder()
                    .id(classroom.getId())
                    .name(classroom.getName())
                    .teacherId(classroom.getTeacherId())
                    .build();
        }
        String url = buildJoinUrl(classroom.getJoinCode());
        return ClassResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .teacherId(classroom.getTeacherId())
                .joinCode(classroom.getJoinCode())
                .joinUrl(url)
                .build();
    }

    private String buildJoinUrl(String code) {
        if (joinUrlBase == null || joinUrlBase.isBlank() || code == null || code.isBlank()) {
            return null;
        }
        String base = joinUrlBase.trim();
        if (base.contains("?")) {
            return base + "&code=" + code;
        }
        return base + "?code=" + code;
    }
}
