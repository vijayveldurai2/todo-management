package com.vijay.todo_management.entity;

import com.vijay.todo_management.enums.Priority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "todos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_todos_project_display_id",
                columnNames = {"project_id", "display_id"}
        )
)
public class Todo {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "display_id", nullable = false, length = 20, updatable = false)
    private String displayId; // e.g. "WR-546" — backend-generated, never reused

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private SprintBoard sprint; // nullable — absence means backlog

    @Column(nullable = false)
    private int position = 0; // drag-and-drop order within status / backlog

    @ManyToMany
    @JoinTable(
            name = "todo_tags",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tags> tags = new HashSet<>();

    // ── Scheduling ──────────────────────────────────────────────────────────
    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    /** Replaces the legacy due_date field. Validated: must not be before startDateTime. */
    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    /** Legacy column retained in DB — application no longer writes to this field. */
    @Column(name = "due_date")
    @Deprecated
    private LocalDateTime dueDate;

    // ── Effort / Planning ────────────────────────────────────────────────────
    /** Decimal hours (e.g. 4.50 = 4h 30m). */
    @Column(name = "estimated_time", precision = 8, scale = 2)
    private BigDecimal estimatedTime;

    /** Decimal hours remaining. Typically decremented as work progresses. */
    @Column(name = "remaining_time", precision = 8, scale = 2)
    private BigDecimal remainingTime;

    /** Story points (integer; scale defined by the team — e.g. Fibonacci). */
    @Column(name = "story_points")
    private Integer storyPoints;

    // ── Assignments ──────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoAssignment> assignments = new ArrayList<>();

    // ── Checklist Items ──────────────────────────────────────────────────────
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    // ── Audit ────────────────────────────────────────────────────────────────
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = this.createdDate;
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
    }
}