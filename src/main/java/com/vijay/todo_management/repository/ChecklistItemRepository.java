package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTodo_IdOrderByPositionAsc(UUID todoId);

    Optional<ChecklistItem> findByIdAndTodo_Id(UUID id, UUID todoId);

    int countByTodo_Id(UUID todoId);

    @Query("SELECT c FROM ChecklistItem c WHERE c.todo.id = :todoId AND c.position BETWEEN :start AND :end ORDER BY c.position ASC")
    List<ChecklistItem> findByTodoIdAndPositionBetween(@Param("todoId") UUID todoId, @Param("start") int start, @Param("end") int end);

    @Query("SELECT c FROM ChecklistItem c WHERE c.todo.id = :todoId AND c.position >= :position ORDER BY c.position ASC")
    List<ChecklistItem> findByTodoIdAndPositionGreaterThanEqual(@Param("todoId") UUID todoId, @Param("position") int position);

    @Query("SELECT c.todo.id AS todoId, COUNT(c) AS totalCount, " +
           "SUM(CASE WHEN c.isChecked = true THEN 1L ELSE 0L END) AS completedCount " +
           "FROM ChecklistItem c WHERE c.todo.id IN :todoIds GROUP BY c.todo.id")
    List<ChecklistStatsProjection> getStatsByTodoIds(@Param("todoIds") Collection<UUID> todoIds);

    @Query("SELECT c.todo.id AS todoId, COUNT(c) AS totalCount, " +
           "SUM(CASE WHEN c.isChecked = true THEN 1L ELSE 0L END) AS completedCount " +
           "FROM ChecklistItem c WHERE c.todo.id = :todoId GROUP BY c.todo.id")
    Optional<ChecklistStatsProjection> getStatsByTodoId(@Param("todoId") UUID todoId);

    void deleteByTodo_Id(UUID todoId);
}
