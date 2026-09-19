package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.TodoRoleCreateRequest;
import com.vijay.todo_management.dto.TodoRoleDto;
import com.vijay.todo_management.dto.TodoRoleUpdateRequest;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.TodoRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manages project-scoped TodoRole labels.
 * All callers must be a project member or workspace SUPER_ADMIN (enforced in service).
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todo-roles")
public class TodoRoleController {

    @Autowired
    private TodoRoleService todoRoleService;

    @GetMapping
    public ResponseEntity<List<TodoRoleDto>> listRoles(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoRoleService.listRoles(workspaceSlug, projectSlug, userId));
    }

    @PostMapping
    public ResponseEntity<TodoRoleDto> createRole(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @Valid @RequestBody TodoRoleCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(
                todoRoleService.createRole(workspaceSlug, projectSlug, request, userId),
                HttpStatus.CREATED);
    }

    @PatchMapping("/{roleId}")
    public ResponseEntity<TodoRoleDto> updateRole(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID roleId,
            @RequestBody TodoRoleUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoRoleService.updateRole(workspaceSlug, projectSlug, roleId, request, userId));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID roleId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        todoRoleService.deleteRole(workspaceSlug, projectSlug, roleId, userId);
        return ResponseEntity.noContent().build();
    }
}
