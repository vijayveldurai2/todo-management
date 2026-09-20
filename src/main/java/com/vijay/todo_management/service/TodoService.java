package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.TodoAssignmentDto;
import com.vijay.todo_management.dto.TodoAssignmentRequest;
import com.vijay.todo_management.dto.TodoCreateRequest;
import com.vijay.todo_management.dto.TodoDto;
import com.vijay.todo_management.dto.TodoStatusUpdateRequest;
import com.vijay.todo_management.dto.TodoUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface TodoService {
    TodoDto resolveTodo(String workspaceSlug, String projectSlug, String displayId, UUID userId);

    List<TodoDto> getSubtasks(String workspaceSlug, String projectSlug, UUID parentId, UUID userId);

    TodoDto promoteSubtask(String workspaceSlug, String projectSlug, UUID todoId, UUID userId);

    TodoDto createTodo(String workspaceSlug, String projectSlug, TodoCreateRequest request, UUID userId);

    List<TodoDto> getTodos(String workspaceSlug, String projectSlug, UUID sprintId, Boolean backlogOnly, UUID userId);

    TodoDto getTodoById(String workspaceSlug, String projectSlug, UUID todoId, UUID userId);

    TodoDto updateTodo(String workspaceSlug, String projectSlug, UUID todoId, TodoUpdateRequest request, UUID userId);

    void deleteTodo(String workspaceSlug, String projectSlug, UUID todoId, UUID userId);

    TodoDto updateTodoStatus(String workspaceSlug, String projectSlug, UUID todoId, TodoStatusUpdateRequest request, UUID userId);

    TodoDto assignSprint(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId);

    TodoDto removeSprintAssignment(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId);

    // ── Assignment management ────────────────────────────────────────────────

    TodoAssignmentDto addAssignment(String workspaceSlug, String projectSlug, UUID todoId, TodoAssignmentRequest request, UUID userId);

    void removeAssignment(String workspaceSlug, String projectSlug, UUID todoId, UUID assignmentId, UUID userId);

    TodoAssignmentDto setPrimaryAssignment(String workspaceSlug, String projectSlug, UUID todoId, UUID assignmentId, UUID userId);
}
