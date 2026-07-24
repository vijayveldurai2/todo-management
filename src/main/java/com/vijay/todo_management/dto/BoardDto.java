package com.vijay.todo_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private Set<Long> memberIds;
}