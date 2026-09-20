package com.vijay.todo_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnCreateRequest {

    @NotBlank(message = "Column name is required")
    private String name;

    @NotNull(message = "primaryStatusId is required")
    private UUID primaryStatusId;

    private Integer position;

    private List<UUID> additionalStatusIds;
}
