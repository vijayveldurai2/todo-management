package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.LoginRequest;
import com.vijay.todo_management.dto.LoginResponse;
import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.dto.UserDto;
import com.vijay.todo_management.dto.VerifyResponse;
import com.vijay.todo_management.entity.PendingSignup;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.entity.UserIdentity;
import com.vijay.todo_management.entity.VerificationToken;
import com.vijay.todo_management.enums.AuthProvider;
import com.vijay.todo_management.enums.LoginMethod;
import com.vijay.todo_management.enums.Plan;
import com.vijay.todo_management.enums.Role;
import com.vijay.todo_management.enums.VerificationPurpose;
import com.vijay.todo_management.repository.PendingSignupRepository;
import com.vijay.todo_management.repository.UserIdentityRepository;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.repository.VerificationTokenRepository;
import com.vijay.todo_management.service.AuthService;
import com.vijay.todo_management.service.EmailService;
import com.vijay.todo_management.service.JwtService;
import com.vijay.todo_management.util.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserIdentityRepository userIdentityRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.vijay.todo_management.security.RedisSessionService redisSessionService;

    public AuthServiceImpl(
            PendingSignupRepository pendingSignupRepository,
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            com.vijay.todo_management.security.RedisSessionService redisSessionService
    ) {
        this.pendingSignupRepository = pendingSignupRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisSessionService = redisSessionService;
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
        pending.setPassword(passwordEncoder.encode(request.getPassword()));
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

        emailService.sendVerificationEmail(email, rawToken);

        return new SignupResponse(
                "Signup successful. Please check your email to verify your account.",
                email
        );
    }

    @Override
    @Transactional
    public VerifyResponse verifyEmail(String rawToken) {
        requireText(rawToken, "token");

        String tokenHash = TokenHasher.sha256(rawToken.trim());
        VerificationToken token = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (token.getPurpose() != VerificationPurpose.EMAIL_VERIFY) {
            throw new RuntimeException("Invalid verification token purpose");
        }
        if (token.getUsedAt() != null) {
            throw new RuntimeException("Verification token has already been used");
        }

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("Verification token has expired");
        }

        PendingSignup pending = token.getPendingSignup();
        if (pending == null) {
            throw new RuntimeException("Verification token is not linked to a pending signup");
        }
        if (pending.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("Pending signup has expired; please sign up again");
        }

        if (userRepository.findByEmail(pending.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered: " + pending.getEmail());
        }
        if (userRepository.findByUsername(pending.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken: " + pending.getUsername());
        }

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setUsername(pending.getUsername());
        user.setPassword(pending.getPassword()); // already BCrypt-hashed at signup
        user.setName(
                pending.getName() != null && !pending.getName().isBlank()
                        ? pending.getName()
                        : pending.getUsername()
        );
        user.setIsActive(true);
        user.setRole(Role.USER);
        user.setPlan(Plan.FREE);
        user.setEmailVerifiedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.PASSWORD);
        identity.setProviderSubject(pending.getEmail());
        identity.setCreatedAt(now);
        userIdentityRepository.save(identity);

        // Keep the token as an audit row: mark used, link to user, unlink from pending
        // so deleting pending does NOT cascade-delete this token.
        UUID pendingId = pending.getId();
        token.setUsedAt(now);
        token.setUser(user);
        token.setPendingSignup(null);
        verificationTokenRepository.saveAndFlush(token);

        pendingSignupRepository.deleteById(pendingId);

        return new VerifyResponse(
                "Email verified successfully. You can now log in.",
                user.getId(),
                user.getEmail(),
                user.getUsername()
        );
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        return login(request, "unknown", "unknown");
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String device) {
        requireText(request.getLogin(), "login");
        requireText(request.getPassword(), "password");

        String login = request.getLogin().trim();
        User user = userRepository.findByEmail(login.toLowerCase())
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new RuntimeException("Invalid login or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account is disabled");
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new RuntimeException("Email is not verified");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid login or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(user, jti);

        // Register active session in Redis allowlist
        redisSessionService.createSession(user.getId(), jti, jwtService.getExpirationSeconds(), device, ip);

        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Override
    public void logout(UUID userId, String jti) {
        if (userId != null && jti != null) {
            redisSessionService.deleteSession(userId, jti);
        }
    }

    @Override
    public void logoutAll(UUID userId) {
        if (userId != null) {
            redisSessionService.deleteAllSessions(userId);
        }
    }

    @Override
    public UserDto me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account is disabled");
        }
        return mapToDto(user);
    }

    private static UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setLocation(user.getLocation());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setActive(user.getIsActive() != null && user.getIsActive());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setPlan(user.getPlan() != null ? user.getPlan().name() : null);
        dto.setEmailVerifiedAt(user.getEmailVerifiedAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        return dto;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(field + " is required");
        }
    }
}

