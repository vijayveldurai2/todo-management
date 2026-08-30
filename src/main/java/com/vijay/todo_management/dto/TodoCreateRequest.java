package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Priority priority;  // defaults to MEDIUM if null

    private UUID statusId;      // optional — defaults to lowest position NOT_STARTED status

    private UUID sprintId;      // optional — null means backlog

    private Set<String> tagNames;

    // ── Scheduling ──────────────────────────────────────────
    /** Validated: startDateTime must not be after endDateTime (400 if violated). */
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;  // replaces dueDate

    // ── Effort / Planning ───────────────────────────────────
    private BigDecimal estimatedTime;   // decimal hours
    private BigDecimal remainingTime;   // decimal hours
    private Integer storyPoints;
}
