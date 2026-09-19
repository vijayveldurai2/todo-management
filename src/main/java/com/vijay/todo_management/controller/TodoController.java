package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}")
public class TodoController {

    @GetMapping("/todos/resolve/{displayId}")
    public ResponseEntity<TodoDto> resolveTodo(@PathVariable String workspaceSlug,
            @PathVariable String projectSlug, @PathVariable String displayId) {
        return ResponseEntity.ok(todoService.resolveTodo(workspaceSlug, projectSlug, displayId,
                CurrentUserProvider.getCurrentUserId()));
    }

    @Autowired
    private TodoService todoService;

    @GetMapping("/todos/{todoId}/subtasks")
    public ResponseEntity<List<TodoDto>> getSubtasks(@PathVariable String workspaceSlug,
            @PathVariable String projectSlug, @PathVariable UUID todoId) {
        return ResponseEntity.ok(todoService.getSubtasks(workspaceSlug, projectSlug, todoId,
                CurrentUserProvider.getCurrentUserId()));
    }

    @DeleteMapping("/todos/{todoId}/parent")
    public ResponseEntity<TodoDto> promoteSubtask(@PathVariable String workspaceSlug,
            @PathVariable String projectSlug, @PathVariable UUID todoId) {
        return ResponseEntity.ok(todoService.promoteSubtask(workspaceSlug, projectSlug, todoId,
                CurrentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/todos")
    public ResponseEntity<TodoDto> createTodo(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @Valid @RequestBody TodoCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(todoService.createTodo(workspaceSlug, projectSlug, request, userId), HttpStatus.CREATED);
    }

    /**
     * Lists todos in the project.
     * Query conflict rule: If both sprintId and backlogOnly are provided together,
     * sprintId takes precedence and backlogOnly is ignored.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<TodoDto>> getTodos(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) Boolean backlogOnly) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.getTodos(workspaceSlug, projectSlug, sprintId, backlogOnly, userId));
    }

    @GetMapping("/todos/{todoId}")
    public ResponseEntity<TodoDto> getTodoById(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.getTodoById(workspaceSlug, projectSlug, todoId, userId));
    }

    @PatchMapping("/todos/{todoId}")
    public ResponseEntity<TodoDto> updateTodo(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @RequestBody TodoUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.updateTodo(workspaceSlug, projectSlug, todoId, request, userId));
    }

    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        todoService.deleteTodo(workspaceSlug, projectSlug, todoId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/todos/{todoId}/status")
    public ResponseEntity<TodoDto> updateTodoStatus(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @Valid @RequestBody TodoStatusUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.updateTodoStatus(workspaceSlug, projectSlug, todoId, request, userId));
    }

    @PostMapping("/sprints/{sprintId}/todos/{todoId}")
    public ResponseEntity<TodoDto> assignSprint(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID sprintId,
            @PathVariable UUID todoId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.assignSprint(workspaceSlug, projectSlug, sprintId, todoId, userId));
    }

    @DeleteMapping("/sprints/{sprintId}/todos/{todoId}")
    public ResponseEntity<TodoDto> removeSprintAssignment(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID sprintId,
            @PathVariable UUID todoId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.removeSprintAssignment(workspaceSlug, projectSlug, sprintId, todoId, userId));
    }

    // ── Assignment management ────────────────────────────────────────────────

    /**
     * Adds a user+role assignment to a todo.
     * If this is the first assignment, isPrimary is forced to true regardless of request.
     * If isPrimary=true is requested, the previous primary (if any) is unset in the same transaction.
     */
    @PostMapping("/todos/{todoId}/assignments")
    public ResponseEntity<com.vijay.todo_management.dto.TodoAssignmentDto> addAssignment(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @jakarta.validation.Valid @RequestBody com.vijay.todo_management.dto.TodoAssignmentRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(
                todoService.addAssignment(workspaceSlug, projectSlug, todoId, request, userId),
                org.springframework.http.HttpStatus.CREATED);
    }

    @DeleteMapping("/todos/{todoId}/assignments/{assignmentId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @PathVariable UUID assignmentId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        todoService.removeAssignment(workspaceSlug, projectSlug, todoId, assignmentId, userId);
        return ResponseEntity.noContent().build();
    }

    /** Sets the specified assignment as primary; unsets the previous primary in the same transaction. */
    @PatchMapping("/todos/{todoId}/assignments/{assignmentId}/primary")
    public ResponseEntity<com.vijay.todo_management.dto.TodoAssignmentDto> setPrimaryAssignment(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID todoId,
            @PathVariable UUID assignmentId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(todoService.setPrimaryAssignment(workspaceSlug, projectSlug, todoId, assignmentId, userId));
    }
}
