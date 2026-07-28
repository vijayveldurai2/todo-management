package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.LoginRequest;
import com.vijay.todo_management.dto.LoginResponse;
import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.dto.VerifyResponse;

public interface AuthService {
    SignupResponse signup(SignupRequest request);

    VerifyResponse verifyEmail(String rawToken);

    LoginResponse login(LoginRequest request);
}
