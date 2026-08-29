package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.StatusCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    private String name;
    private StatusCategory category;
    private Integer position;
}
