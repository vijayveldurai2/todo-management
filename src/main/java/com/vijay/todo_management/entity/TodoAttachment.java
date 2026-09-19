package com.vijay.todo_management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "todo_attachments") @Getter @Setter
public class TodoAttachment {
    @Id @GeneratedValue @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;
    // SET NULL on Todo deletion queues physical cleanup without losing the storage key.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id")
    private Todo todo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private User uploadedBy;
    @Column(name = "storage_key", nullable = false, unique = true, updatable = false, length = 36)
    private String storageKey;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(nullable = false)
    private boolean deleted;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}

