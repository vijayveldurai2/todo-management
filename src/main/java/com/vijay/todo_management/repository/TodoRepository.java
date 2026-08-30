package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    List<Todo> findByProject_Id(UUID projectId);

    List<Todo> findByProject_IdAndSprint_Id(UUID projectId, UUID sprintId);

    List<Todo> findByProject_IdAndSprintIsNull(UUID projectId);

    Optional<Todo> findByIdAndProject_Id(UUID id, UUID projectId);

    Optional<Todo> findByIdAndProject_Workspace_SlugAndProject_Slug(UUID id, String workspaceSlug, String projectSlug);
}
