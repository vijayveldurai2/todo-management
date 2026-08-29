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
 * Admin-defined role within a project.
 * Each project has its own set of roles (e.g. "Developer", "Tester", "Lead").
 * Roles are created by the project admin and can differ across projects.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "project_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_roles_project_name",
                columnNames = {"project_id", "name"}
        )
)
public class ProjectRole {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    /** Human-readable role name defined by the project admin, e.g. "Developer", "Tester". */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * When true, members with this role have project-admin privileges
     * (can manage members, roles, boards, etc.).
     */
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt  = LocalDateTime.now();
        this.updatedAt  = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
