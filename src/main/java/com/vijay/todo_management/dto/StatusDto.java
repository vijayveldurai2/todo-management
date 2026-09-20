package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.StatusCategory;
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
public class StatusDto {
    private UUID id;
    private UUID projectId;
    private String name;
    private StatusCategory category;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
