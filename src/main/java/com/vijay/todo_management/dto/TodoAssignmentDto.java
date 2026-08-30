package com.vijay.todo_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoAssignmentDto {
    private UUID id;
    private UUID todoId;
    private UUID userId;
    private String userDisplayName; // user.name or user.username
    private String userEmail;
    private String userAvatarUrl;
    private UUID todoRoleId;
    private String todoRoleName;
    private boolean isPrimary;
    private LocalDateTime createdAt;
}
