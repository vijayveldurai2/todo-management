package com.vijay.todo_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private boolean isDefault;
}
