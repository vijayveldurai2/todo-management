package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ProjectDto;
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
            @RequestBody ProjectDto dto,
            @RequestParam UUID userId) {
        return new ResponseEntity<>(projectService.createProject(workspaceSlug, dto, userId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(
            @PathVariable String workspaceSlug,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(projectService.getProjectsByWorkspace(workspaceSlug, userId));
    }

    @GetMapping("/{projectSlug}")
    public ResponseEntity<ProjectDto> getProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(projectService.getProjectBySlug(workspaceSlug, projectSlug, userId));
    }

    @PatchMapping("/{projectSlug}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @RequestBody ProjectDto dto,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(projectService.updateProject(workspaceSlug, projectSlug, dto, userId));
    }

    @DeleteMapping("/{projectSlug}")
    public ResponseEntity<Void> archiveProject(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @RequestParam UUID userId) {
        projectService.archiveProject(workspaceSlug, projectSlug, userId);
        return ResponseEntity.noContent().build();
    }
}
