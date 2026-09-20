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
public class ChecklistItemDto {
    private UUID id;
    private UUID todoId;
    private String text;
    private boolean isChecked;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
