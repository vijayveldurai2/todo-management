package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.ChecklistItemDto;
import com.vijay.todo_management.dto.ChecklistItemUpdateRequest;
import com.vijay.todo_management.dto.ItemReorderRequest;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.ChecklistItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/checklist-items")
public class ChecklistItemController {

    @Autowired
    private ChecklistItemService checklistItemService;

    @GetMapping
    public ResponseEntity<List<ChecklistItemDto>> getChecklistItems(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(checklistItemService.getChecklistItems(workspaceSlug, projectSlug, todoId, userId));
    }

    @PostMapping
    public ResponseEntity<ChecklistItemDto> addItem(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @Valid @RequestBody ChecklistItemCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(
                checklistItemService.addItem(workspaceSlug, projectSlug, todoId, request, userId),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ChecklistItemDto> updateItem(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @PathVariable UUID itemId,
            @RequestBody ChecklistItemUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(
                checklistItemService.updateItem(workspaceSlug, projectSlug, todoId, itemId, request, userId)
        );
    }

    @PatchMapping("/{itemId}/reorder")
    public ResponseEntity<ChecklistItemDto> reorderItem(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemReorderRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(
                checklistItemService.reorderItem(workspaceSlug, projectSlug, todoId, itemId, request.getPosition(), userId)
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @PathVariable UUID itemId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        checklistItemService.deleteItem(workspaceSlug, projectSlug, todoId, itemId, userId);
        return ResponseEntity.noContent().build();
    }
}
