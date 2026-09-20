package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ProjectRoleDto;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.ProjectRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectSlug}/roles")
public class ProjectRoleController {

    @Autowired
    private ProjectRoleService projectRoleService;

    @GetMapping
    public ResponseEntity<List<ProjectRoleDto>> getRoles(
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectRoleService.getRoles(projectSlug, userId));
    }

    @PostMapping
    public ResponseEntity<ProjectRoleDto> createRole(
            @PathVariable String projectSlug,
            @RequestBody ProjectRoleDto dto) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(projectRoleService.createRole(projectSlug, dto, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/{roleId}")
    public ResponseEntity<ProjectRoleDto> updateRole(
            @PathVariable String projectSlug,
            @PathVariable UUID roleId,
            @RequestBody ProjectRoleDto dto) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectRoleService.updateRole(projectSlug, roleId, dto, userId));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable String projectSlug,
            @PathVariable UUID roleId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        projectRoleService.deleteRole(projectSlug, roleId, userId);
        return ResponseEntity.noContent().build();
    }
}
