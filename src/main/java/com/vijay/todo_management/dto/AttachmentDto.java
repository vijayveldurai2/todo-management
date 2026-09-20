package com.vijay.todo_management.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record AttachmentDto(UUID id, UUID todoId, String fileName, long sizeBytes,
        UUID uploadedBy, String uploaderDisplayName, LocalDateTime createdAt) {}

