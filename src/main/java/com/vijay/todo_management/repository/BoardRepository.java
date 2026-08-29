package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByProject_Id(UUID projectId);

    List<Board> findByProject_Workspace_SlugAndProject_Slug(String workspaceSlug, String projectSlug);

    Optional<Board> findByIdAndProject_Workspace_SlugAndProject_Slug(UUID id, String workspaceSlug, String projectSlug);

    Optional<Board> findByIdAndProject_Id(UUID id, UUID projectId);
}