package com.vijay.todo_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnDto {
    private UUID id;
    private UUID boardId;
    private String name;
    private int position;
    private UUID primaryStatusId;
    private StatusDto primaryStatus;
    private List<StatusDto> additionalStatuses = new ArrayList<>();
    private List<TodoDto> todos = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
