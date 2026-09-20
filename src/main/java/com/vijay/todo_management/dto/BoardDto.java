package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.enums.SprintStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private UUID id;
    private UUID projectId;
    private String name;
    private BoardType boardType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sprint-specific fields
    private LocalDate sprintStartDate;
    private LocalDate sprintEndDate;
    private String sprintGoal;
    private SprintStatus sprintStatus;

    // Columns included in detail response
    private List<BoardColumnDto> columns = new ArrayList<>();
}