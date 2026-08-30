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
 * Admin-defined, project-scoped role label for todo assignments.
 * Examples: "Developer", "QA", "Supervisor", "Reviewer".
 * These are purely descriptive — they carry NO authorization semantics.
 * Do not use TodoRole for permission checks; use ProjectRole / WorkspaceMember.Role for that.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "todo_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_todo_roles_project_name",
                columnNames = {"project_id", "name"}
        )
)
public class TodoRole {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    /** Human-readable role label, unique per project (case-insensitive enforced at service layer). */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
