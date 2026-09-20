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
public class ProjectMemberDto {
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private String userName;     // denormalized for display
    private String userEmail;    // denormalized for display
    private UUID projectRoleId;  // the assigned role's ID
    private String roleName;     // the assigned role's name (admin-defined)
    private boolean roleIsAdmin; // whether this role has project-admin privileges
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}
