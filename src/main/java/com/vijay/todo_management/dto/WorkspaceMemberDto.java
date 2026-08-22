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
public class WorkspaceMemberDto {
    private UUID id;
    private UUID workspaceId;
    private UUID userId;
    private String userName;   // denormalized for display, avoids a second lookup on the frontend
    private String userEmail;
    private String role;       // SUPER_ADMIN, USER
    private String status;     // ACTIVE, INVITED, REMOVED
    private LocalDateTime createdAt;
}
