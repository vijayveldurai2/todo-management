package com.vijay.todo_management.dto;

import com.vijay.todo_management.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdateRequest {

    private String title;
    private String description;
    private Priority priority;
    private Set<String> tagNames;

    // ── Scheduling (validated: startDateTime must not be after endDateTime) ──
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;  // replaces dueDate

    // ── Effort / Planning ────────────────────────────────────────────────────
    private BigDecimal estimatedTime;   // decimal hours
    private BigDecimal remainingTime;   // decimal hours
    private Integer storyPoints;
}
