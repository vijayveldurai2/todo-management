package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodoDto {
    private UUID id;
    private String title;
    private String description;
    private Boolean completed;
    private Priority priority;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private LocalDateTime dueDate;
    private LocalDateTime completedDate;
    private Set<String> tagNames;
    private UUID boardId;
}
