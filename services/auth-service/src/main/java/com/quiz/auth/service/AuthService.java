package com.quiz.auth.service;

import com.quiz.auth.dto.AuthResponse;
import com.quiz.auth.dto.LoginRequest;
import com.quiz.auth.dto.RegistrationPendingResponse;
import com.quiz.auth.dto.RegisterRequest;
import com.quiz.auth.dto.ResendVerificationRequest;
import com.quiz.auth.dto.VerifyEmailRequest;
import com.quiz.auth.dto.UserLookupResponse;
import com.quiz.auth.dto.UserProfileResponse;

public interface AuthService {
    RegistrationPendingResponse register(RegisterRequest request);

    RegistrationPendingResponse resendVerificationCode(ResendVerificationRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    UserProfileResponse getProfile(String username);

    UserLookupResponse lookupUserByEmail(String email);

    UserLookupResponse lookupUserById(String userId);
}
