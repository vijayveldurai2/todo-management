package com.vijay.todo_management.repository;
import com.vijay.todo_management.entity.Comment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTodo_IdOrderByCreatedAtAscIdAsc(UUID todoId);
    Optional<Comment> findByIdAndTodo_Id(UUID id, UUID todoId);
    // Called only while holding the Todo write lock; no new replies can race cleanup.
    @Modifying(flushAutomatically = true)
    @Query("update Comment c set c.parentComment = null where c.todo.id = :todoId")
    int detachParentsForTodo(@Param("todoId") UUID todoId);
}

