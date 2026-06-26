package com.quiz.auth.service;

import com.quiz.auth.dto.AuthResponse;
import com.quiz.auth.dto.LoginRequest;
import com.quiz.auth.dto.RegistrationPendingResponse;
import com.quiz.auth.dto.RegisterRequest;
import com.quiz.auth.dto.ResendVerificationRequest;
import com.quiz.auth.dto.VerifyEmailRequest;
import com.quiz.auth.dto.UserLookupResponse;
import com.quiz.auth.dto.UserProfileResponse;
import com.quiz.auth.event.EmailVerificationRequestedEvent;
import com.quiz.auth.entity.User;
import com.quiz.auth.exception.BadRequestException;
import com.quiz.auth.exception.ResourceNotFoundException;
import com.quiz.auth.repository.UserRepository;
import com.quiz.auth.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RabbitTemplate rabbitTemplate;
    private final String rabbitExchange;
    private final String rabbitRoutingKey;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange:exam.events}") String rabbitExchange,
            @Value("${app.rabbitmq.auth-user-registered-routing-key:auth.user.registered}") String rabbitRoutingKey
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitExchange = rabbitExchange;
        this.rabbitRoutingKey = rabbitRoutingKey;
    }

    @Override
    @Transactional
    public RegistrationPendingResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        return issueAndPublishVerificationCode(saved);
        }

        @Override
        @Transactional
        public RegistrationPendingResponse resendVerificationCode(ResendVerificationRequest request) {
        String email = request.getEmail().trim();
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified. Please login.");
        }

        return issueAndPublishVerificationCode(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailVerified() == null || !user.getEmailVerified()) {
            throw new BadRequestException("Email not verified. Please verify your email first.");
        }

        String token = jwtService.generateToken(user.getUsername(), Map.of(
                "role", user.getRole().name(),
                "userId", user.getId(),
                "email", user.getEmail()
        ));
        return new AuthResponse(token, "Bearer", 86400,
                new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt()));
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().trim();
        String code = request.getCode().trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instant now = Instant.now();
        Instant expiresAt = user.getEmailVerificationExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(now)) {
            throw new BadRequestException("Verification code expired. Please register again.");
        }

        String expectedHash = user.getEmailVerificationCodeHash();
        String providedHash = sha256Hex(code);
        if (expectedHash == null || !expectedHash.equals(providedHash)) {
            throw new BadRequestException("Invalid verification code");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCodeHash(null);
        user.setEmailVerificationExpiresAt(null);
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getUsername(), Map.of(
                "role", saved.getRole().name(),
                "userId", saved.getId(),
                "email", saved.getEmail()
        ));

        return new AuthResponse(token, "Bearer", 86400,
                new UserProfileResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getCreatedAt()));
    }

    @Override
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    @Override
    public UserLookupResponse lookupUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("email is required");
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserLookupResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    public UserLookupResponse lookupUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserLookupResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private RegistrationPendingResponse issueAndPublishVerificationCode(User user) {
        String verificationCode = generateVerificationCode();
        Instant expiresAt = Instant.now().plusSeconds(10 * 60); // 10 minutes

        user.setEmailVerificationCodeHash(sha256Hex(verificationCode));
        user.setEmailVerificationExpiresAt(expiresAt);
        User saved = userRepository.save(user);

        EmailVerificationRequestedEvent event = new EmailVerificationRequestedEvent(
            UUID.randomUUID().toString(),
            saved.getId(),
            saved.getEmail(),
            verificationCode,
            expiresAt
        );
        rabbitTemplate.convertAndSend(rabbitExchange, rabbitRoutingKey, event);

        return new RegistrationPendingResponse(
            saved.getEmail(),
            expiresAt,
            "Verification code sent to your email. Please verify to activate your account."
        );
        }

    private String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash verification code", ex);
        }
    }
}
