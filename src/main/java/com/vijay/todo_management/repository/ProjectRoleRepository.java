package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID> {

    /** All roles defined for a given project, ordered by name. */
    List<ProjectRole> findByProjectIdOrderByNameAsc(UUID projectId);

    /** Check whether a role name already exists in a project (for duplicate validation). */
    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
