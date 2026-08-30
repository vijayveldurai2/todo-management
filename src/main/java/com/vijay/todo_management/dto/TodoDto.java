package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoDto {
    private UUID id;
    private UUID projectId;
    private String displayId;   // e.g. "WR-546"
    private String title;
    private String description;
    private Priority priority;
    private UUID statusId;
    private StatusDto status;
    private UUID sprintId;      // nullable — absence means backlog
    private int position;
    private Set<String> tagNames;

    // Derived completion state (true only when status.category == DONE)
    private Boolean isDone;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private LocalDateTime dueDate;
}