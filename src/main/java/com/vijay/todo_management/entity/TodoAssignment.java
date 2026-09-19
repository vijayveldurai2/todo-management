package com.vijay.todo_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Associates a user with a todo under a specific project-defined role label.
 *
 * Uniqueness: a user cannot hold the exact same TodoRole twice on one todo
 * ({@code uk_todo_assignments_todo_user_role}), but CAN hold multiple DIFFERENT
 * roles on the same todo (separate rows).
 *
 * Primary flag: at most one assignment per todo may have {@code isPrimary = true}.
 * This is enforced transactionally at the service layer — not via a DB constraint.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "todo_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_todo_assignments_todo_user_role",
                columnNames = {"todo_id", "user_id", "todo_role_id"}
        )
)
public class TodoAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false, updatable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_role_id", nullable = false)
    private TodoRole todoRole;

    /**
     * True if this is the primary assignee for the todo.
     * At most one assignment per todo may hold this flag — enforced by service logic.
     */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
