package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.WorkspaceInviteDto;
import com.vijay.todo_management.dto.WorkspaceInviteRequestDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;
import com.vijay.todo_management.service.WorkspaceInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceInviteController {

    @Autowired
    private WorkspaceInviteService workspaceInviteService;

    // TODO: userId/inviterId are temporary stand-ins for the authenticated principal.
    // Replace with SecurityContextHolder once Spring Security is wired in.

    @PostMapping("/{workspaceId}/invites")
    public ResponseEntity<WorkspaceInviteDto> createInvite(
            @PathVariable UUID workspaceId,
            @RequestParam UUID inviterId,
            @RequestBody WorkspaceInviteRequestDto req) {
        return new ResponseEntity<>(workspaceInviteService.createInvite(workspaceId, inviterId, req), HttpStatus.CREATED);
    }

    @GetMapping("/{workspaceId}/invites")
    public ResponseEntity<List<WorkspaceInviteDto>> getPendingInvites(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(workspaceInviteService.getPendingInvites(workspaceId));
    }

    @GetMapping("/invites/mine")
    public ResponseEntity<List<WorkspaceInviteDto>> getMyPendingInvites(@RequestParam UUID userId) {
        return ResponseEntity.ok(workspaceInviteService.getMyPendingInvites(userId));
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<WorkspaceMemberDto> acceptInvite(
            @PathVariable UUID inviteId,
            @RequestParam UUID acceptingUserId) {
        return ResponseEntity.ok(workspaceInviteService.acceptInvite(inviteId, acceptingUserId));
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<Void> declineInvite(
            @PathVariable UUID inviteId,
            @RequestParam UUID decliningUserId) {
        workspaceInviteService.declineInvite(inviteId, decliningUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(@PathVariable UUID inviteId) {
        workspaceInviteService.revokeInvite(inviteId);
        return ResponseEntity.noContent().build();
    }

    // Token-based variants, for the raw email-link path
    @PostMapping("/invites/accept-by-token")
    public ResponseEntity<WorkspaceMemberDto> acceptInviteByToken(
            @RequestParam String token,
            @RequestParam UUID acceptingUserId) {
        return ResponseEntity.ok(workspaceInviteService.acceptInviteByToken(token, acceptingUserId));
    }

    @PostMapping("/invites/decline-by-token")
    public ResponseEntity<Void> declineInviteByToken(@RequestParam String token) {
        workspaceInviteService.declineInviteByToken(token);
        return ResponseEntity.noContent().build();
    }
}
