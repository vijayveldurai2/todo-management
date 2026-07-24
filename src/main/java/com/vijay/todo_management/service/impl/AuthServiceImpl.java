package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.entity.PendingSignup;
import com.vijay.todo_management.entity.VerificationToken;
import com.vijay.todo_management.enums.LoginMethod;
import com.vijay.todo_management.enums.VerificationPurpose;
import com.vijay.todo_management.repository.PendingSignupRepository;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.repository.VerificationTokenRepository;
import com.vijay.todo_management.service.AuthService;
import com.vijay.todo_management.service.EmailService;
import com.vijay.todo_management.util.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int PENDING_SIGNUP_TTL_HOURS = 24;
    private static final int VERIFICATION_TOKEN_TTL_HOURS = 24;

    private final PendingSignupRepository pendingSignupRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AuthServiceImpl(
            PendingSignupRepository pendingSignupRepository,
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.pendingSignupRepository = pendingSignupRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        requireText(request.getEmail(), "email");
        requireText(request.getUsername(), "username");
        requireText(request.getPassword(), "password");

        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (userRepository.findByEmail(email).isPresent() || pendingSignupRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered or pending verification: " + email);
        }
        if (userRepository.findByUsername(username).isPresent() || pendingSignupRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken: " + username);
        }

        LocalDateTime now = LocalDateTime.now();

        PendingSignup pending = new PendingSignup();
        pending.setEmail(email);
        pending.setUsername(username);
        // Password hashing comes with Spring Security later; store as-is for now.
        pending.setPassword(request.getPassword());
        pending.setName(request.getName() != null ? request.getName().trim() : null);
        pending.setPrimaryLoginMethod(LoginMethod.PASSWORD);
        pending.setCreatedAt(now);
        pending.setExpiresAt(now.plusHours(PENDING_SIGNUP_TTL_HOURS));
        pending = pendingSignupRepository.save(pending);

        String rawToken = UUID.randomUUID().toString();

        VerificationToken token = new VerificationToken();
        token.setPendingSignup(pending);
        token.setTokenHash(TokenHasher.sha256(rawToken));
        token.setPurpose(VerificationPurpose.EMAIL_VERIFY);
        token.setExpiresAt(now.plusHours(VERIFICATION_TOKEN_TTL_HOURS));
        token.setCreatedAt(now);
        verificationTokenRepository.save(token);

        // Send SMTP mail when enabled; otherwise logs the link for local testing.
        emailService.sendVerificationEmail(email, rawToken);

        return new SignupResponse(
                "Signup successful. Please check your email to verify your account.",
                email
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(field + " is required");
        }
    }
}
