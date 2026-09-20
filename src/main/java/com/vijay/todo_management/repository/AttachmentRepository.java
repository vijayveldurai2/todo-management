package com.vijay.todo_management.repository;
import com.vijay.todo_management.entity.TodoAttachment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface AttachmentRepository extends JpaRepository<TodoAttachment, UUID> {
    @EntityGraph(attributePaths = "uploadedBy")
    List<TodoAttachment> findByTodo_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(UUID todoId);
    Optional<TodoAttachment> findByIdAndTodo_IdAndDeletedFalse(UUID id, UUID todoId);
    @Query("select a from TodoAttachment a where a.deleted = true or a.todo is null")
    List<TodoAttachment> findCleanupCandidates(Pageable page);
    boolean existsByStorageKey(String key);
}

