package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID> {
    List<ProjectRole> findByProject_Slug(String projectSlug);
    Optional<ProjectRole> findByProject_SlugAndId(String projectSlug, UUID roleId);
    boolean existsByProject_IdAndName(UUID projectId, String name);
    long countByProject_IdAndIsAdminTrue(UUID projectId);
}
