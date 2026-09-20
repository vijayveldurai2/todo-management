package com.vijay.todo_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoAssignmentRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "todoRoleId is required")
    private UUID todoRoleId;

    /**
     * Optional. If true, this assignment becomes the primary assignee for the todo
     * and the previous primary (if any) is unset in the same transaction.
     * Automatically forced to true when this is the very first assignment on the todo.
     */
    private Boolean isPrimary;
}
