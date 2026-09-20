package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ProjectMemberDto;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.ProjectMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectSlug}/members")
public class ProjectMemberController {

    @Autowired
    private ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<List<ProjectMemberDto>> getMembers(
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(projectMemberService.getMembers(projectSlug, userId));
    }

    @PostMapping
    public ResponseEntity<ProjectMemberDto> addMember(
            @PathVariable String projectSlug,
            @RequestBody Map<String, String> body) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        UUID targetUserId = UUID.fromString(body.get("userId"));
        UUID roleId = null;
        if (body.containsKey("roleId") && body.get("roleId") != null) {
            roleId = UUID.fromString(body.get("roleId"));
        }
        
        return new ResponseEntity<>(projectMemberService.addMember(projectSlug, targetUserId, roleId, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/{targetUserId}")
    public ResponseEntity<ProjectMemberDto> changeMemberRole(
            @PathVariable String projectSlug,
            @PathVariable UUID targetUserId,
            @RequestBody Map<String, String> body) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        UUID roleId = UUID.fromString(body.get("roleId"));
        return ResponseEntity.ok(projectMemberService.changeMemberRole(projectSlug, targetUserId, roleId, userId));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String projectSlug,
            @PathVariable UUID targetUserId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        projectMemberService.removeMember(projectSlug, targetUserId, userId);
        return ResponseEntity.noContent().build();
    }
}
