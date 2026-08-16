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
    private Boolean completed;
    private UUID boardId;       // nullable
    private UUID columnId;
    private int position;
    private Set<String> tagNames;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private LocalDateTime dueDate;
    private LocalDateTime completedDate;
}