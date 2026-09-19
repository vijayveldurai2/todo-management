package com.vijay.todo_management.dto;

public record RefreshSession(TokenRefreshResponse response, String rawRefreshToken) {
}
