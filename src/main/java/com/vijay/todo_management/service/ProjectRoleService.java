package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ProjectRoleDto;

import java.util.List;
import java.util.UUID;

public interface ProjectRoleService {
    List<ProjectRoleDto> getRoles(String projectSlug, UUID currentUserId);
    ProjectRoleDto createRole(String projectSlug, ProjectRoleDto dto, UUID currentUserId);
    ProjectRoleDto updateRole(String projectSlug, UUID roleId, ProjectRoleDto dto, UUID currentUserId);
    void deleteRole(String projectSlug, UUID roleId, UUID currentUserId);
}
