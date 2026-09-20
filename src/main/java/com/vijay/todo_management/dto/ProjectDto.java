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
public class ProjectDto {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private String slug;
    private String description;
    private String prefixCode;   // e.g. "WR" — immutable after creation
    private String status;       // ACTIVE or ARCHIVED
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // displayIdSeq intentionally omitted — internal counter, not exposed to the frontend
}
