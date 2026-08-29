package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspace_Slug(String workspaceSlug);
    Optional<Project> findByWorkspace_SlugAndSlug(String workspaceSlug, String slug);
    Optional<Project> findFirstBySlug(String slug);
    boolean existsByWorkspace_IdAndSlug(UUID workspaceId, String slug);
}
