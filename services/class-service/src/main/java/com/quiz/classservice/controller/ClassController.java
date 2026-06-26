package com.quiz.classservice.controller;

import com.quiz.classservice.dto.AddUserToClassRequest;
import com.quiz.classservice.dto.BaseResponse;
import com.quiz.classservice.dto.ClassResponse;
import com.quiz.classservice.dto.CreateClassRequest;
import com.quiz.classservice.dto.JoinClassRequest;
import com.quiz.classservice.dto.StudentResponse;
import com.quiz.classservice.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Class", description = "Class management endpoints")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @PostMapping("/classes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Create class")
    @ApiResponse(responseCode = "201", description = "Class created")
    public ResponseEntity<BaseResponse<ClassResponse>> createClass(
            @Valid @RequestBody CreateClassRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String teacherId = jwt.getClaimAsString("userId");
        ClassResponse data = classService.createClass(request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED.value(), "Class created", data));
    }

    @GetMapping("/classes")
    @Operation(summary = "List classes")
    @ApiResponse(responseCode = "200", description = "Classes retrieved")
    public ResponseEntity<BaseResponse<List<ClassResponse>>> getClasses() {
        List<ClassResponse> data = classService.getAllClasses();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Classes retrieved", data));
    }

    @GetMapping("/classes/{id}")
    @Operation(summary = "Get class details (join code visible only to the class teacher)")
    @ApiResponse(responseCode = "200", description = "Class retrieved")
    public ResponseEntity<BaseResponse<ClassResponse>> getClassById(
            @PathVariable("id") String classId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String viewerId = jwt != null ? jwt.getClaimAsString("userId") : null;
        ClassResponse data = classService.getClassById(classId, viewerId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Class retrieved", data));
    }

    @PostMapping("/classes/join")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Join class by join code (student self-enroll)")
    @ApiResponse(responseCode = "200", description = "Joined class")
    public ResponseEntity<BaseResponse<ClassResponse>> joinClassByCode(
            @Valid @RequestBody JoinClassRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("userId");
        String email = jwt.getClaimAsString("email");
        ClassResponse data = classService.joinClassByCode(request, userId, email);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Joined class", data));
    }

    @PostMapping("/classes/{id}/regenerate-join-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Regenerate join code (class teacher only)")
    @ApiResponse(responseCode = "200", description = "Join code regenerated")
    public ResponseEntity<BaseResponse<ClassResponse>> regenerateJoinCode(
            @PathVariable("id") String classId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actorId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        ClassResponse data = classService.regenerateJoinCode(classId, actorId, role);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Join code regenerated", data));
    }

    @PostMapping("/classes/{id}/add-user")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Add user to class")
    @ApiResponse(responseCode = "200", description = "User added to class")
    public ResponseEntity<BaseResponse<Void>> addUserToClass(
            @PathVariable("id") String classId,
            @Valid @RequestBody AddUserToClassRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actorId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        classService.addUserToClass(classId, request, actorId, role);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "User added to class", null));
    }

    @DeleteMapping("/classes/{id}/students/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Remove student from class (class teacher or admin)")
    @ApiResponse(responseCode = "200", description = "User removed")
    public ResponseEntity<BaseResponse<Void>> removeStudentFromClass(
            @PathVariable("id") String classId,
            @PathVariable("userId") String studentUserId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actorId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        classService.removeUserFromClass(classId, studentUserId, actorId, role);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "User removed from class", null));
    }

    @GetMapping("/classes/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Operation(summary = "Get students by class (class teacher or admin)")
    @ApiResponse(responseCode = "200", description = "Students retrieved")
    public ResponseEntity<BaseResponse<List<StudentResponse>>> getStudentsByClassId(
            @PathVariable("id") String classId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actorId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        List<StudentResponse> data = classService.getStudentsByClassId(classId, actorId, role);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Students retrieved", data));
    }

    @GetMapping("/users/{userId}/classes")
    @Operation(summary = "Get classes by user")
    @ApiResponse(responseCode = "200", description = "Classes retrieved")
    public ResponseEntity<BaseResponse<List<ClassResponse>>> getClassesByUserId(@PathVariable String userId) {
        List<ClassResponse> data = classService.getClassesByUserId(userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Classes retrieved", data));
    }
}
