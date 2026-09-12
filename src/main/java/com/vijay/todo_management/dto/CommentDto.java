package com.vijay.todo_management.dto;
import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;
public record CommentDto(UUID id, UUID todoId, UUID authorId,
        String authorDisplayName, String authorAvatarUrl, UUID parentCommentId,
        JsonNode contentJson, String contentPlainText, boolean isDeleted,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
