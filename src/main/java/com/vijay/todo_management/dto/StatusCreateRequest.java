package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.StatusCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusCreateRequest {
    @NotBlank(message = "Status name is required")
    private String name;

    @NotNull(message = "Status category is required")
    private StatusCategory category;

    private Integer position;
}
