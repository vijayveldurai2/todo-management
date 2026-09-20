package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.SprintStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequest {
    private String name;

    // Sprint-specific fields
    private LocalDate sprintStartDate;
    private LocalDate sprintEndDate;
    private String sprintGoal;
    private SprintStatus sprintStatus;
}
