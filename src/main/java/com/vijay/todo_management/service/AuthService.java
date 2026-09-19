package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.LoginRequest;
import com.vijay.todo_management.dto.LoginResponse;
import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.dto.UserDto;
import com.vijay.todo_management.dto.VerifyResponse;

public interface AuthService {
    SignupResponse signup(SignupRequest request);

    VerifyResponse verifyEmail(String rawToken);

    LoginResponse login(LoginRequest request);

    LoginResponse login(LoginRequest request, String ip, String device);

    com.vijay.todo_management.dto.AuthSession loginSession(LoginRequest request, String ip, String device);

    com.vijay.todo_management.dto.RefreshSession refresh(String rawRefreshToken, String ip, String device);

    /** Returns the authenticated user's profile. Throws if user not found or inactive. */
    UserDto me(java.util.UUID userId);

    void logout(java.util.UUID userId, String jti);

    void logout(java.util.UUID userId, String jti, String rawRefreshToken);

    void logoutAll(java.util.UUID userId);

    void logoutAll(java.util.UUID userId, String rawRefreshToken);
}

