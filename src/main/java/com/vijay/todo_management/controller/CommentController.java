package com.vijay.todo_management.controller;
import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/comments")
public class CommentController {
    private final CommentService service;
    @GetMapping
    public List<CommentDto> list(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId) {
        return service.list(workspaceSlug, projectSlug, todoId, CurrentUserProvider.getCurrentUserId());
    }
    @PostMapping
    public ResponseEntity<CommentDto> create(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId, @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(201).body(service.create(workspaceSlug, projectSlug, todoId, request,
                CurrentUserProvider.getCurrentUserId()));
    }
    @PatchMapping("/{commentId}")
    public CommentDto edit(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId, @PathVariable UUID commentId, @RequestBody CommentUpdateRequest request) {
        return service.edit(workspaceSlug, projectSlug, todoId, commentId, request, CurrentUserProvider.getCurrentUserId());
    }
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId, @PathVariable UUID commentId) {
        service.delete(workspaceSlug, projectSlug, todoId, commentId, CurrentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
