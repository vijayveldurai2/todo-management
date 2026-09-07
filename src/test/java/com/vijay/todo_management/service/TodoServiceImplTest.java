package com.vijay.todo_management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.Priority;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.mapper.TodoMapper;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.TodoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private SprintBoardRepository sprintBoardRepository;

    @Mock
    private TagsRepository tagsRepository;

    @Spy
    private TodoMapper todoMapper = new TodoMapper();

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @InjectMocks
    private TodoServiceImpl todoService;

    private UUID userId;
    private UUID nonMemberUserId;
    private UUID superAdminUserId;
    private Workspace workspace;
    private Project project;
    private Project otherProject;
    private Status statusTodo;
    private Status statusInProgress;
    private Status statusDone;
    private Status otherProjectStatus;
    private SprintBoard sprintBoard;
    private SprintBoard otherProjectSprint;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        nonMemberUserId = UUID.randomUUID();
        superAdminUserId = UUID.randomUUID();

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setSlug("test-ws");

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setSlug("test-proj");
        project.setWorkspace(workspace);
        project.setPrefixCode("WR");
        project.setDisplayIdSeq(5);

        otherProject = new Project();
        otherProject.setId(UUID.randomUUID());
        otherProject.setSlug("other-proj");
        otherProject.setWorkspace(workspace);
        otherProject.setPrefixCode("OTH");
        otherProject.setDisplayIdSeq(0);

        statusTodo = new Status();
        statusTodo.setId(UUID.randomUUID());
        statusTodo.setProject(project);
        statusTodo.setName("To Do");
        statusTodo.setCategory(StatusCategory.NOT_STARTED);
        statusTodo.setPosition(0);

        statusInProgress = new Status();
        statusInProgress.setId(UUID.randomUUID());
        statusInProgress.setProject(project);
        statusInProgress.setName("In Progress");
        statusInProgress.setCategory(StatusCategory.IN_PROGRESS);
        statusInProgress.setPosition(1);

        statusDone = new Status();
        statusDone.setId(UUID.randomUUID());
        statusDone.setProject(project);
        statusDone.setName("Done");
        statusDone.setCategory(StatusCategory.DONE);
        statusDone.setPosition(2);

        otherProjectStatus = new Status();
        otherProjectStatus.setId(UUID.randomUUID());
        otherProjectStatus.setProject(otherProject);
        otherProjectStatus.setName("Other Status");
        otherProjectStatus.setCategory(StatusCategory.NOT_STARTED);
        otherProjectStatus.setPosition(0);

        sprintBoard = new SprintBoard();
        sprintBoard.setId(UUID.randomUUID());
        sprintBoard.setProject(project);
        sprintBoard.setName("Sprint 1");

        otherProjectSprint = new SprintBoard();
        otherProjectSprint.setId(UUID.randomUUID());
        otherProjectSprint.setProject(otherProject);
        otherProjectSprint.setName("Other Sprint");
    }

    private void mockProjectAccess(UUID callerId) {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), callerId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), callerId)).thenReturn(true);
    }

    private void mockSuperAdminAccess(UUID callerId) {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        WorkspaceMember sa = new WorkspaceMember();
        sa.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), callerId)).thenReturn(Optional.of(sa));
    }

    @Test
    void testCreateTodo_DefaultsToLowestPositionNotStartedStatus_AndSetsDisplayId() {
        mockProjectAccess(userId);
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo, statusInProgress, statusDone));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("New Task");
        req.setPriority(Priority.HIGH);

        TodoDto result = todoService.createTodo("test-ws", "test-proj", req, userId);

        assertNotNull(result);
        assertEquals("New Task", result.getTitle());
        assertEquals("WR-6", result.getDisplayId());
        assertEquals(statusTodo.getId(), result.getStatusId());
        assertEquals(false, result.getIsDone());
        assertEquals(6, project.getDisplayIdSeq());
        verify(projectRepository).save(project);
    }

    @Test
    void testCreateTodo_WithExplicitStatus() {
        mockProjectAccess(userId);
        when(statusRepository.findByIdAndProject_Id(statusInProgress.getId(), project.getId()))
                .thenReturn(Optional.of(statusInProgress));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("In Progress Task");
        req.setStatusId(statusInProgress.getId());

        TodoDto result = todoService.createTodo("test-ws", "test-proj", req, userId);

        assertNotNull(result);
        assertEquals(statusInProgress.getId(), result.getStatusId());
        assertEquals(false, result.getIsDone());
    }

    @Test
    void testCreateTodo_WithDoneStatus_DerivesIsDoneTrue() {
        mockProjectAccess(userId);
        when(statusRepository.findByIdAndProject_Id(statusDone.getId(), project.getId()))
                .thenReturn(Optional.of(statusDone));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Completed Task");
        req.setStatusId(statusDone.getId());

        TodoDto result = todoService.createTodo("test-ws", "test-proj", req, userId);

        assertNotNull(result);
        assertEquals(true, result.getIsDone());
    }

    @Test
    void testCreateTodo_WithCrossProjectStatus_ThrowsBadRequest() {
        mockProjectAccess(userId);
        when(statusRepository.findByIdAndProject_Id(otherProjectStatus.getId(), project.getId()))
                .thenReturn(Optional.empty());

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Invalid Status Task");
        req.setStatusId(otherProjectStatus.getId());

        assertThrows(BadRequestException.class, () ->
                todoService.createTodo("test-ws", "test-proj", req, userId));
    }

    @Test
    void testCreateTodo_WithCrossProjectSprint_ThrowsBadRequest() {
        mockProjectAccess(userId);
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo));
        when(sprintBoardRepository.findById(otherProjectSprint.getId()))
                .thenReturn(Optional.of(otherProjectSprint));

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Sprint Task");
        req.setSprintId(otherProjectSprint.getId());

        assertThrows(BadRequestException.class, () ->
                todoService.createTodo("test-ws", "test-proj", req, userId));
    }

    @Test
    void testCreateTodo_NonMember_ThrowsForbidden() {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), nonMemberUserId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), nonMemberUserId)).thenReturn(false);

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Denied Task");

        assertThrows(ForbiddenException.class, () ->
                todoService.createTodo("test-ws", "test-proj", req, nonMemberUserId));
    }

    @Test
    void testGetTodos_ConflictPrecedence_SprintIdOverridesBacklogOnly() {
        mockProjectAccess(userId);
        UUID sprintId = sprintBoard.getId();

        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setProject(project);
        todo.setTitle("Sprint Item");
        todo.setStatus(statusTodo);
        todo.setSprint(sprintBoard);

        when(todoRepository.findByProject_IdAndSprint_Id(project.getId(), sprintId))
                .thenReturn(List.of(todo));

        // When both sprintId AND backlogOnly=true are passed:
        List<TodoDto> results = todoService.getTodos("test-ws", "test-proj", sprintId, true, userId);

        assertEquals(1, results.size());
        assertEquals("Sprint Item", results.get(0).getTitle());
        verify(todoRepository).findByProject_IdAndSprint_Id(project.getId(), sprintId);
        verify(todoRepository, never()).findByProject_IdAndSprintIsNull(any());
        verify(todoRepository, never()).findByProject_Id(any());
    }

    @Test
    void testGetTodos_BacklogOnly() {
        mockProjectAccess(userId);

        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setProject(project);
        todo.setTitle("Backlog Item");
        todo.setStatus(statusTodo);
        todo.setSprint(null);

        when(todoRepository.findByProject_IdAndSprintIsNull(project.getId()))
                .thenReturn(List.of(todo));

        List<TodoDto> results = todoService.getTodos("test-ws", "test-proj", null, true, userId);

        assertEquals(1, results.size());
        assertEquals("Backlog Item", results.get(0).getTitle());
        verify(todoRepository).findByProject_IdAndSprintIsNull(project.getId());
    }

    @Test
    void testUpdateTodoStatus_Success() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setTitle("Task");
        todo.setStatus(statusTodo);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(statusRepository.findByIdAndProject_Id(statusDone.getId(), project.getId())).thenReturn(Optional.of(statusDone));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoStatusUpdateRequest req = new TodoStatusUpdateRequest(statusDone.getId());
        TodoDto result = todoService.updateTodoStatus("test-ws", "test-proj", todoId, req, userId);

        assertEquals(statusDone.getId(), result.getStatusId());
        assertEquals(true, result.getIsDone());
    }

    @Test
    void testUpdateTodoStatus_CrossProjectStatus_ThrowsBadRequest() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(statusRepository.findByIdAndProject_Id(otherProjectStatus.getId(), project.getId())).thenReturn(Optional.empty());

        TodoStatusUpdateRequest req = new TodoStatusUpdateRequest(otherProjectStatus.getId());
        assertThrows(BadRequestException.class, () ->
                todoService.updateTodoStatus("test-ws", "test-proj", todoId, req, userId));
    }

    @Test
    void testAssignSprint_Success_AndReassign() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        SprintBoard sprint2 = new SprintBoard();
        sprint2.setId(UUID.randomUUID());
        sprint2.setProject(project);
        sprint2.setName("Sprint 2");

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);
        todo.setSprint(sprintBoard);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(sprintBoardRepository.findById(sprint2.getId())).thenReturn(Optional.of(sprint2));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoDto result = todoService.assignSprint("test-ws", "test-proj", sprint2.getId(), todoId, userId);

        assertNotNull(result);
        assertEquals(sprint2.getId(), result.getSprintId());
    }

    @Test
    void testAssignSprint_CrossProjectSprint_ThrowsResourceNotFound() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(sprintBoardRepository.findById(otherProjectSprint.getId())).thenReturn(Optional.of(otherProjectSprint));

        assertThrows(ResourceNotFoundException.class, () ->
                todoService.assignSprint("test-ws", "test-proj", otherProjectSprint.getId(), todoId, userId));
    }

    @Test
    void testRemoveSprintAssignment_Success() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);
        todo.setSprint(sprintBoard);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoDto result = todoService.removeSprintAssignment("test-ws", "test-proj", sprintBoard.getId(), todoId, userId);

        assertNotNull(result);
        assertNull(result.getSprintId());
    }

    @Test
    void testRemoveSprintAssignment_NotAssignedToSprint_ThrowsBadRequest() {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);
        todo.setSprint(null); // in backlog

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));

        assertThrows(BadRequestException.class, () ->
                todoService.removeSprintAssignment("test-ws", "test-proj", sprintBoard.getId(), todoId, userId));
    }

    @Test
    void testSuperAdminBypass_CanPerformAllOperations() {
        mockSuperAdminAccess(superAdminUserId);
        UUID todoId = UUID.randomUUID();

        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setProject(project);
        todo.setStatus(statusTodo);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(todo));
        when(statusRepository.findByIdAndProject_Id(statusDone.getId(), project.getId())).thenReturn(Optional.of(statusDone));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoStatusUpdateRequest req = new TodoStatusUpdateRequest(statusDone.getId());
        TodoDto result = todoService.updateTodoStatus("test-ws", "test-proj", todoId, req, superAdminUserId);

        assertNotNull(result);
        assertEquals(statusDone.getId(), result.getStatusId());
    }

    @Test
    void testCreateTodo_BothDescriptionNull_Succeeds() {
        mockProjectAccess(userId);
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Task Null Description");
        req.setDescriptionJson(null);
        req.setDescriptionPlainText(null);

        TodoDto result = todoService.createTodo("test-ws", "test-proj", req, userId);
        assertNotNull(result);
        assertNull(result.getDescriptionJson());
        assertNull(result.getDescriptionPlainText());
    }

    @Test
    void testCreateTodo_OnlyJsonProvided_ThrowsBadRequest() throws Exception {
        mockProjectAccess(userId);
        JsonNode jsonNode = new ObjectMapper().readTree("{\"type\":\"doc\"}");

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Task Inconsistent");
        req.setDescriptionJson(jsonNode);
        req.setDescriptionPlainText(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                todoService.createTodo("test-ws", "test-proj", req, userId));
        assertEquals("descriptionJson and descriptionPlainText must either both be provided or both be null", ex.getMessage());
    }

    @Test
    void testCreateTodo_OnlyPlainTextProvided_ThrowsBadRequest() {
        mockProjectAccess(userId);

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Task Inconsistent");
        req.setDescriptionJson(null);
        req.setDescriptionPlainText("Some text");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                todoService.createTodo("test-ws", "test-proj", req, userId));
        assertEquals("descriptionJson and descriptionPlainText must either both be provided or both be null", ex.getMessage());
    }

    @Test
    void testCreateTodo_BothProvided_Succeeds() throws Exception {
        mockProjectAccess(userId);
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        JsonNode jsonNode = new ObjectMapper().readTree("{\"type\":\"doc\",\"content\":[]}");

        TodoCreateRequest req = new TodoCreateRequest();
        req.setTitle("Rich Task");
        req.setDescriptionJson(jsonNode);
        req.setDescriptionPlainText("Plain text extract");

        TodoDto result = todoService.createTodo("test-ws", "test-proj", req, userId);
        assertNotNull(result);
        assertEquals(jsonNode, result.getDescriptionJson());
        assertEquals("Plain text extract", result.getDescriptionPlainText());
    }

    @Test
    void testUpdateTodo_OnlyTitleChanged_PreservesExistingDescription() throws Exception {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();
        JsonNode existingJson = new ObjectMapper().readTree("{\"type\":\"doc\",\"text\":\"original\"}");

        Todo existingTodo = new Todo();
        existingTodo.setId(todoId);
        existingTodo.setProject(project);
        existingTodo.setTitle("Original Title");
        existingTodo.setDescriptionJson(existingJson);
        existingTodo.setDescriptionPlainText("original text");

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(existingTodo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        // PATCH request omitting description fields entirely (presence flags remain false)
        TodoUpdateRequest req = new TodoUpdateRequest();
        req.setTitle("Updated Title Only");
        assertFalse(req.isDescriptionUpdateRequested());

        TodoDto result = todoService.updateTodo("test-ws", "test-proj", todoId, req, userId);

        assertNotNull(result);
        assertEquals("Updated Title Only", result.getTitle());
        // Verify existing description is completely preserved and untouched
        assertEquals(existingJson, result.getDescriptionJson());
        assertEquals("original text", result.getDescriptionPlainText());
    }

    @Test
    void testUpdateTodo_ExplicitClear_SetsDescriptionToNull() throws Exception {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();
        JsonNode existingJson = new ObjectMapper().readTree("{\"type\":\"doc\"}");

        Todo existingTodo = new Todo();
        existingTodo.setId(todoId);
        existingTodo.setProject(project);
        existingTodo.setTitle("Title");
        existingTodo.setDescriptionJson(existingJson);
        existingTodo.setDescriptionPlainText("text");

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(existingTodo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        // PATCH request explicitly clearing description
        TodoUpdateRequest req = new TodoUpdateRequest();
        req.setDescriptionJson(null);
        req.setDescriptionPlainText(null);

        assertTrue(req.isDescriptionUpdateRequested());

        TodoDto result = todoService.updateTodo("test-ws", "test-proj", todoId, req, userId);

        assertNotNull(result);
        assertNull(result.getDescriptionJson());
        assertNull(result.getDescriptionPlainText());
    }

    @Test
    void testUpdateTodo_InconsistentUpdate_ThrowsBadRequest() throws Exception {
        mockProjectAccess(userId);
        UUID todoId = UUID.randomUUID();
        Todo existingTodo = new Todo();
        existingTodo.setId(todoId);
        existingTodo.setProject(project);

        when(todoRepository.findByIdAndProject_Id(todoId, project.getId())).thenReturn(Optional.of(existingTodo));

        TodoUpdateRequest req = new TodoUpdateRequest();
        req.setDescriptionJson(new ObjectMapper().readTree("{\"type\":\"doc\"}"));
        // plain text omitted

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                todoService.updateTodo("test-ws", "test-proj", todoId, req, userId));
        assertEquals("descriptionJson and descriptionPlainText must either both be provided or both be null", ex.getMessage());
    }
}
