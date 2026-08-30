package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ProjectDto;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @PathVariable String workspaceSlug,
            @RequestBody ProjectDto dto) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(projectService.createProject(workspaceSlug, dto, userId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(
            @PathVariable String workspaceSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProjectsByWorkspace(workspaceSlug, userId));
    }

    @GetMapping("/{projectSlug}")
    public ResponseEntity<ProjectDto> getProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProjectBySlug(workspaceSlug, projectSlug, userId));
    }

    @PatchMapping("/{projectSlug}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @RequestBody ProjectDto dto) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectService.updateProject(workspaceSlug, projectSlug, dto, userId));
    }

    @DeleteMapping("/{projectSlug}")
    public ResponseEntity<Void> archiveProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        projectService.archiveProject(workspaceSlug, projectSlug, userId);
        return ResponseEntity.noContent().build();
    }
}
