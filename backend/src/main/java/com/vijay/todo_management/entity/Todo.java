package com.vijay.todo_management.entity;

import com.vijay.todo_management.enums.Priority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @ManyToOne
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
    private Priority priority;

    @Column(nullable = false)
    private Boolean completed;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board; // nullable — ON DELETE SET NULL at the DB level

    @ManyToOne
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    @Column(nullable = false)
    private int position = 0; // drag-and-drop order within column

    @ManyToMany
    @JoinTable(
            name = "todo_tags",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tags> tags = new HashSet<>();

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

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