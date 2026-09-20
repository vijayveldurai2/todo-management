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
 * Maps a user to a project with a dynamic, admin-defined role.
 * A user's role can differ per project; roles are defined via {@link ProjectRole}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "project_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_members_project_user",
                columnNames = {"project_id", "user_id"}
        )
)
public class ProjectMember {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * The project-specific role for this member.
     * Set by the project admin; can be changed without removing/re-adding the member.
     */
    @ManyToOne
    @JoinColumn(name = "project_role_id", nullable = false)
    private ProjectRole projectRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt   = LocalDateTime.now();
        this.createdAt  = this.joinedAt;
        this.updatedAt  = this.joinedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}