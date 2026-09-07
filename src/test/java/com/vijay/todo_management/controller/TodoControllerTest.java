package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.enums.Priority;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.TodoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoController todoController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("user@example.com")
                .username("user")
                .jti("jti-1")
                .active(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateTodo_ReturnsCreated() {
        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Task Title");
        req.setDescriptionPlainText("Desc");
        req.setPriority(Priority.MEDIUM);
        TodoDto dto = new TodoDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Task Title");

        when(todoService.createTodo("ws-1", "proj-1", req, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.createTodo("ws-1", "proj-1", req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Task Title", response.getBody().getTitle());
        verify(todoService).createTodo("ws-1", "proj-1", req, userId);
    }

    @Test
    void testGetTodos_ReturnsOk() {
        UUID sprintId = UUID.randomUUID();
        TodoDto dto = new TodoDto();
        dto.setId(UUID.randomUUID());

        when(todoService.getTodos("ws-1", "proj-1", sprintId, true, userId)).thenReturn(List.of(dto));

        ResponseEntity<List<TodoDto>> response = todoController.getTodos("ws-1", "proj-1", sprintId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(todoService).getTodos("ws-1", "proj-1", sprintId, true, userId);
    }

    @Test
    void testGetTodoById_ReturnsOk() {
        UUID todoId = UUID.randomUUID();
        TodoDto dto = new TodoDto();
        dto.setId(todoId);

        when(todoService.getTodoById("ws-1", "proj-1", todoId, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.getTodoById("ws-1", "proj-1", todoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(todoId, response.getBody().getId());
        verify(todoService).getTodoById("ws-1", "proj-1", todoId, userId);
    }

    @Test
    void testUpdateTodo_ReturnsOk() {
        UUID todoId = UUID.randomUUID();
        TodoUpdateRequest req = new TodoUpdateRequest();
        req.setTitle("Updated Title");
        req.setDescriptionPlainText("Updated Desc");
        req.setPriority(Priority.HIGH);
        TodoDto dto = new TodoDto();
        dto.setId(todoId);
        dto.setTitle("Updated Title");

        when(todoService.updateTodo("ws-1", "proj-1", todoId, req, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.updateTodo("ws-1", "proj-1", todoId, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Title", response.getBody().getTitle());
        verify(todoService).updateTodo("ws-1", "proj-1", todoId, req, userId);
    }

    @Test
    void testDeleteTodo_ReturnsNoContent() {
        UUID todoId = UUID.randomUUID();

        ResponseEntity<Void> response = todoController.deleteTodo("ws-1", "proj-1", todoId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(todoService).deleteTodo("ws-1", "proj-1", todoId, userId);
    }

    @Test
    void testUpdateTodoStatus_ReturnsOk() {
        UUID todoId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        TodoStatusUpdateRequest req = new TodoStatusUpdateRequest(statusId);
        TodoDto dto = new TodoDto();
        dto.setId(todoId);
        dto.setStatusId(statusId);

        when(todoService.updateTodoStatus("ws-1", "proj-1", todoId, req, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.updateTodoStatus("ws-1", "proj-1", todoId, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(statusId, response.getBody().getStatusId());
        verify(todoService).updateTodoStatus("ws-1", "proj-1", todoId, req, userId);
    }

    @Test
    void testAssignSprint_ReturnsOk() {
        UUID todoId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        TodoDto dto = new TodoDto();
        dto.setId(todoId);
        dto.setSprintId(sprintId);

        when(todoService.assignSprint("ws-1", "proj-1", sprintId, todoId, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.assignSprint("ws-1", "proj-1", sprintId, todoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sprintId, response.getBody().getSprintId());
        verify(todoService).assignSprint("ws-1", "proj-1", sprintId, todoId, userId);
    }

    @Test
    void testRemoveSprintAssignment_ReturnsOk() {
        UUID todoId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        TodoDto dto = new TodoDto();
        dto.setId(todoId);
        dto.setSprintId(null);

        when(todoService.removeSprintAssignment("ws-1", "proj-1", sprintId, todoId, userId)).thenReturn(dto);

        ResponseEntity<TodoDto> response = todoController.removeSprintAssignment("ws-1", "proj-1", sprintId, todoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(todoService).removeSprintAssignment("ws-1", "proj-1", sprintId, todoId, userId);
    }
}
