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
public class WorkspaceInviteDto {
    private UUID id;
    private UUID workspaceId;
    private String workspaceName;
    private String workspaceDescription;
    private String email;
    private UUID invitedBy;
    private String role;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
