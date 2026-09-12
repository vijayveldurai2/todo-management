package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Todo t where t.id = :id and t.project.id = :projectId")
    Optional<Todo> findForCommentWrite(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("projectId") UUID projectId);

    List<Todo> findByProject_Id(UUID projectId);

    List<Todo> findByProject_IdAndSprint_Id(UUID projectId, UUID sprintId);

    List<Todo> findByProject_IdAndSprintIsNull(UUID projectId);

    Optional<Todo> findByIdAndProject_Id(UUID id, UUID projectId);

    Optional<Todo> findByIdAndProject_Workspace_SlugAndProject_Slug(UUID id, String workspaceSlug, String projectSlug);
}
