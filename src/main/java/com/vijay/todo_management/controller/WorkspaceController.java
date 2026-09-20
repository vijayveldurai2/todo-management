package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.WorkspaceDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;
import com.vijay.todo_management.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vijay.todo_management.security.CurrentUserProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceDto> createWorkspace(@RequestBody WorkspaceDto dto) {
        UUID creatorId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(workspaceService.createWorkspace(dto, creatorId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceDto>> getWorkspacesForCurrentUser() {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(workspaceService.getWorkspacesForUser(userId));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceDto> getWorkspaceById(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(workspaceService.getWorkspaceById(workspaceId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<WorkspaceDto> getWorkspaceBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(workspaceService.getWorkspaceBySlug(slug));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceDto> updateWorkspace(
            @PathVariable UUID workspaceId,
            @RequestBody WorkspaceDto dto) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(workspaceId, dto));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMemberDto>> getMembers(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(workspaceService.getMembers(workspaceId));
    }


    @PutMapping("/{workspaceId}/members/{userId}/role")
    public ResponseEntity<WorkspaceMemberDto> changeMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(workspaceService.changeMemberRole(workspaceId, userId, body.get("role")));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId) {
        workspaceService.removeMember(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
