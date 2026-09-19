package com.vijay.todo_management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemCreateRequest {

    @NotBlank(message = "Text is required")
    private String text;

    @Min(value = 0, message = "Position must be greater than or equal to 0")
    private Integer position;
}
