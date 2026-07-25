package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;

public interface AuthService {
    SignupResponse signup(SignupRequest request);
}
