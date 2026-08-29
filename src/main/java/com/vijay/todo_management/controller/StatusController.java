package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.StatusCreateRequest;
import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.dto.StatusUpdateRequest;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.StatusService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/statuses")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping
    public ResponseEntity<List<StatusDto>> getStatuses(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(statusService.getStatuses(workspaceSlug, projectSlug, userId));
    }

    @PostMapping
    public ResponseEntity<StatusDto> createStatus(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @Valid @RequestBody StatusCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(statusService.createStatus(workspaceSlug, projectSlug, request, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/{statusId}")
    public ResponseEntity<StatusDto> updateStatus(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID statusId,
            @RequestBody StatusUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(statusService.updateStatus(workspaceSlug, projectSlug, statusId, request, userId));
    }

    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID statusId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        statusService.deleteStatus(workspaceSlug, projectSlug, statusId, userId);
        return ResponseEntity.noContent().build();
    }
}
