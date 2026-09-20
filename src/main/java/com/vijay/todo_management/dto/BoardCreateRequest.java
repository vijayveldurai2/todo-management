package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.enums.SprintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequest {

    @NotBlank(message = "Board name is required")
    private String name;

    @NotNull(message = "boardType is required (KANBAN or SPRINT)")
    private BoardType boardType;

    // Sprint-specific fields
    private LocalDate sprintStartDate;
    private LocalDate sprintEndDate;
    private String sprintGoal;
    private SprintStatus sprintStatus;
}
