package com.vijay.todo_management.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.vijay.todo_management.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoDto {
    private UUID id;
    private UUID projectId;
    private UUID parentTodoId;
    private String displayId;   // e.g. "WR-546"
    private String title;
    @tools.jackson.databind.annotation.JsonSerialize(using = TodoJsonBridge.Writer.class)
    private JsonNode descriptionJson;
    private String descriptionPlainText;
    private Priority priority;
    private UUID statusId;
    private StatusDto status;
    private UUID sprintId;      // nullable — absence means backlog
    private int position;
    private Set<String> tagNames;

    // ── Scheduling ──────────────────────────────────────────
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;  // replaces dueDate

    // ── Effort / Planning ───────────────────────────────────
    private BigDecimal estimatedTime;   // decimal hours, e.g. 4.50 = 4h 30m
    private BigDecimal remainingTime;   // decimal hours remaining
    private Integer storyPoints;        // team-defined integer (e.g. Fibonacci)

    // ── Assignments ─────────────────────────────────────────
    private List<TodoAssignmentDto> assignments = new ArrayList<>();

    // ── Derived ─────────────────────────────────────────────
    /** true only when status.category == DONE */
    private Boolean isDone;

    // ── Checklist Statistics ─────────────────────────────────
    private Integer checklistTotalCount;
    private Integer checklistCompletedCount;
    /** Nullable: null when checklistTotalCount == 0; rounded percentage (0-100) otherwise */
    private Integer checklistProgressPercentage;

    // ── Audit ────────────────────────────────────────────────
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
