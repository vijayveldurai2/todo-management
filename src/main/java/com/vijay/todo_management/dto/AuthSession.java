package com.vijay.todo_management.dto;

public record AuthSession(LoginResponse loginResponse, String rawRefreshToken) {
}
