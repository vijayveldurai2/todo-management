package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.TodoRoleCreateRequest;
import com.vijay.todo_management.dto.TodoRoleDto;
import com.vijay.todo_management.dto.TodoRoleUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface TodoRoleService {

    List<TodoRoleDto> listRoles(String workspaceSlug, String projectSlug, UUID userId);

    TodoRoleDto createRole(String workspaceSlug, String projectSlug, TodoRoleCreateRequest request, UUID userId);

    TodoRoleDto updateRole(String workspaceSlug, String projectSlug, UUID roleId, TodoRoleUpdateRequest request, UUID userId);

    void deleteRole(String workspaceSlug, String projectSlug, UUID roleId, UUID userId);
}
