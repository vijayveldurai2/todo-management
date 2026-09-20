package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.TodoAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoAssignmentRepository extends JpaRepository<TodoAssignment, UUID> {

    List<TodoAssignment> findByTodo_Id(UUID todoId);

    Optional<TodoAssignment> findByTodo_IdAndIsPrimaryTrue(UUID todoId);

    Optional<TodoAssignment> findByIdAndTodo_Id(UUID id, UUID todoId);

    boolean existsByTodoRole_Id(UUID todoRoleId);

    long countByTodo_Id(UUID todoId);
}
