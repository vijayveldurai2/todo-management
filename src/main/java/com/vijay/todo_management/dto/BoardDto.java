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
public class BoardDto {
    private UUID id;
    private UUID projectId;
    private String name;
    private String slug;
    private String description;
    private String type;     // BOARD, SPRINT
    private int position;
}