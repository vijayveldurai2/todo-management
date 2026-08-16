package com.vijay.todo_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "board_columns")
public class BoardColumn {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int position; // drag-and-drop order within board

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false; // marks the 4 seeded columns (Todo, Working, Testing, Done)

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