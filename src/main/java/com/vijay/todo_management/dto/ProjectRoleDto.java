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
public class ProjectRoleDto {
    private UUID id;
    private UUID projectId;
    private String name;
    private boolean isAdmin;
    private LocalDateTime createdAt;
}
