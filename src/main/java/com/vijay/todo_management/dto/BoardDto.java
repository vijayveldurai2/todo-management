package com.vijay.todo_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private Set<UUID> memberIds;
}
