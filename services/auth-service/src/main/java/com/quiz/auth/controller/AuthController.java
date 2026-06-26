package com.quiz.auth.controller;

import com.quiz.auth.dto.AuthResponse;
import com.quiz.auth.dto.BaseResponse;
import com.quiz.auth.dto.LoginRequest;
import com.quiz.auth.dto.RegistrationPendingResponse;
import com.quiz.auth.dto.RegisterRequest;
import com.quiz.auth.dto.ResendVerificationRequest;
import com.quiz.auth.dto.VerifyEmailRequest;
import com.quiz.auth.dto.UserLookupResponse;
import com.quiz.auth.dto.UserProfileResponse;
import com.quiz.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login and profile endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Registers a new user and returns a JWT")
    @ApiResponse(responseCode = "201", description = "User registered")
    public ResponseEntity<BaseResponse<RegistrationPendingResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationPendingResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED.value(), "User registered", data));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code", description = "Sends a new verification code for unverified users")
    @ApiResponse(responseCode = "200", description = "Verification code resent")
    public ResponseEntity<BaseResponse<RegistrationPendingResponse>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        RegistrationPendingResponse data = authService.resendVerificationCode(request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Verification code resent", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and returns a JWT")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Login successful", data));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies email using the code sent via email and returns JWT")
    @ApiResponse(responseCode = "200", description = "Email verified")
    public ResponseEntity<BaseResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse data = authService.verifyEmail(request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Email verified", data));
    }

    @GetMapping("/me")
    @Operation(summary = "Get authenticated profile", description = "Returns profile of current authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    public ResponseEntity<BaseResponse<UserProfileResponse>> me(org.springframework.security.core.Authentication authentication) {
        UserProfileResponse data = authService.getProfile(authentication.getName());
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Profile retrieved", data));
    }

    @GetMapping("/lookup-user")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Resolve userId by email (for adding students to class, etc.)")
    @ApiResponse(responseCode = "200", description = "User found")
    public ResponseEntity<BaseResponse<UserLookupResponse>> lookupUserByEmail(@RequestParam("email") String email) {
        UserLookupResponse data = authService.lookupUserByEmail(email);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "User found", data));
    }

    @GetMapping("/lookup-user-by-id")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Operation(summary = "Lookup user by id for showing class members")
    @ApiResponse(responseCode = "200", description = "User found")
    public ResponseEntity<BaseResponse<UserLookupResponse>> lookupUserById(@RequestParam("userId") String userId) {
        UserLookupResponse data = authService.lookupUserById(userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "User found", data));
    }
}
