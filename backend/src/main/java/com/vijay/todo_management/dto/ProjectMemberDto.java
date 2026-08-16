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
    private String userName;   // denormalized for display
    private String userEmail;
    private String role;       // USER, DEVELOPER, TESTER, LEAD
    private LocalDateTime createdAt;

    // Note: SUPER_ADMIN never appears here — enforced via workspace-level auth check,
    // no project_members row is ever created for SA, per earlier design decision.
}
