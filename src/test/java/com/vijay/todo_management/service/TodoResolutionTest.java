package com.vijay.todo_management.service;

import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.mapper.TodoMapper;
import com.vijay.todo_management.service.impl.TodoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoResolutionTest {
    TodoServiceImpl service = new TodoServiceImpl();
    ProjectRepository projects = mock(ProjectRepository.class);
    TodoRepository todos = mock(TodoRepository.class);
    ProjectMemberRepository members = mock(ProjectMemberRepository.class);
    UUID user = UUID.randomUUID(), projectId = UUID.randomUUID();
    TodoResolutionTest() {
        ReflectionTestUtils.setField(service, "projectRepository", projects);
        ReflectionTestUtils.setField(service, "todoRepository", todos);
        ReflectionTestUtils.setField(service, "projectMemberRepository", members);
        ReflectionTestUtils.setField(service, "workspaceMemberRepository", mock(WorkspaceMemberRepository.class));
        ReflectionTestUtils.setField(service, "checklistItemRepository", mock(ChecklistItemRepository.class));
        ReflectionTestUtils.setField(service, "todoMapper", mock(TodoMapper.class));
    }
    Project scope() {
        Workspace workspace = new Workspace(); workspace.setId(UUID.randomUUID()); workspace.setSlug("bakery");
        Project project = new Project(); project.setId(projectId); project.setSlug("orders"); project.setWorkspace(workspace);
        when(projects.findByWorkspace_SlugAndSlug("bakery", "orders")).thenReturn(Optional.of(project));
        when(members.existsByProject_IdAndUser_Id(projectId, user)).thenReturn(true);
        return project;
    }
    @Test void malformedIdsAndUppercaseSlugsNeverReachDatabase() {
        for (String id : List.of("ORD-12", "ord-x", "ord-12-extra", "_", "settings", "ord-")) {
            assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("bakery", "orders", id, user));
        }
        assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("Bakery", "orders", "ord-12", user));
        assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("bakery", "Orders", "ord-12", user));
        verifyNoInteractions(projects, todos);
    }
    @Test void resolutionUsesAuthorizedProjectAndExactStoredId() {
        scope(); Todo todo = new Todo(); todo.setId(UUID.randomUUID()); todo.setDisplayId("ord-12");
        when(todos.findByProject_IdAndDisplayId(projectId, "ord-12")).thenReturn(Optional.of(todo));
        service.resolveTodo("bakery", "orders", "ord-12", user);
        verify(todos).findByProject_IdAndDisplayId(projectId, "ord-12");
        todo.setDisplayId("ORD-12");
        assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("bakery", "orders", "ord-12", user));
    }
    @Test void nonMembersCannotResolveTodo() {
        scope(); when(members.existsByProject_IdAndUser_Id(projectId, user)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.resolveTodo("bakery", "orders", "ord-12", user));
        verifyNoInteractions(todos);
    }
    @Test void missingIdAndCollationMismatchAreNotFound() {
        Project project = scope();
        assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("bakery", "orders", "ord-12", user));
        project.setSlug("Orders");
        assertThrows(ResourceNotFoundException.class, () -> service.resolveTodo("bakery", "orders", "ord-12", user));
    }
}
