package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.enums.SprintStatus;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.mapper.TodoMapper;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.BoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TodoRepository todoRepository;

    @Spy
    private TodoMapper todoMapper = new TodoMapper();

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @InjectMocks
    private BoardServiceImpl boardService;

    private UUID userId;
    private UUID nonMemberUserId;
    private Workspace workspace;
    private Project project;
    private Project otherProject;
    private Status statusTodo;
    private Status statusInProgress;
    private Status statusDone;
    private Status otherProjectStatus;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        nonMemberUserId = UUID.randomUUID();

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setSlug("test-ws");

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setSlug("test-proj");
        project.setWorkspace(workspace);

        otherProject = new Project();
        otherProject.setId(UUID.randomUUID());
        otherProject.setSlug("other-proj");
        otherProject.setWorkspace(workspace);

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
    }

    private void mockSuperAdminAccess(UUID uid) {
        WorkspaceMember wm = new WorkspaceMember();
        wm.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), uid)).thenReturn(Optional.of(wm));
    }

    @Test
    void testCreateKanbanBoard_AutoCreatesThreeColumnsWithDefaultStatuses() {
        mockSuperAdminAccess(userId);

        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> {
            Board b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo, statusInProgress, statusDone));

        BoardCreateRequest req = new BoardCreateRequest();
        req.setName("Kanban Board 1");
        req.setBoardType(BoardType.KANBAN);

        BoardDto created = boardService.createBoard("test-ws", "test-proj", req, userId);

        assertNotNull(created);
        assertEquals("Kanban Board 1", created.getName());
        assertEquals(BoardType.KANBAN, created.getBoardType());

        ArgumentCaptor<List<BoardColumn>> columnCaptor = ArgumentCaptor.forClass(List.class);
        verify(boardColumnRepository).saveAll(columnCaptor.capture());

        List<BoardColumn> seededColumns = columnCaptor.getValue();
        assertEquals(3, seededColumns.size());

        assertEquals("To Do", seededColumns.get(0).getName());
        assertEquals(statusTodo.getId(), seededColumns.get(0).getPrimaryStatus().getId());
        assertEquals(0, seededColumns.get(0).getPosition());

        assertEquals("In Progress", seededColumns.get(1).getName());
        assertEquals(statusInProgress.getId(), seededColumns.get(1).getPrimaryStatus().getId());
        assertEquals(1, seededColumns.get(1).getPosition());

        assertEquals("Done", seededColumns.get(2).getName());
        assertEquals(statusDone.getId(), seededColumns.get(2).getPrimaryStatus().getId());
        assertEquals(2, seededColumns.get(2).getPosition());
    }

    @Test
    void testCreateSprintBoard_WithSprintFields() {
        mockSuperAdminAccess(userId);

        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> {
            Board b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo, statusInProgress, statusDone));

        BoardCreateRequest req = new BoardCreateRequest();
        req.setName("Sprint 1");
        req.setBoardType(BoardType.SPRINT);
        req.setSprintStartDate(LocalDate.of(2026, 9, 1));
        req.setSprintEndDate(LocalDate.of(2026, 9, 15));
        req.setSprintGoal("Launch MVP");
        req.setSprintStatus(SprintStatus.ACTIVE);

        BoardDto created = boardService.createBoard("test-ws", "test-proj", req, userId);

        assertNotNull(created);
        assertEquals(BoardType.SPRINT, created.getBoardType());
        assertEquals(LocalDate.of(2026, 9, 1), created.getSprintStartDate());
        assertEquals(LocalDate.of(2026, 9, 15), created.getSprintEndDate());
        assertEquals("Launch MVP", created.getSprintGoal());
        assertEquals(SprintStatus.ACTIVE, created.getSprintStatus());
    }

    @Test
    void testMultipleBoardsShareSameStatusInstances() {
        mockSuperAdminAccess(userId);

        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> {
            Board b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo, statusInProgress, statusDone));

        BoardCreateRequest req1 = new BoardCreateRequest("Board A", BoardType.KANBAN, null, null, null, null);
        BoardCreateRequest req2 = new BoardCreateRequest("Board B", BoardType.KANBAN, null, null, null, null);

        boardService.createBoard("test-ws", "test-proj", req1, userId);
        boardService.createBoard("test-ws", "test-proj", req2, userId);

        ArgumentCaptor<List<BoardColumn>> columnCaptor = ArgumentCaptor.forClass(List.class);
        verify(boardColumnRepository, times(2)).saveAll(columnCaptor.capture());

        List<List<BoardColumn>> allInvocations = columnCaptor.getAllValues();
        assertEquals(statusTodo.getId(), allInvocations.get(0).get(0).getPrimaryStatus().getId());
        assertEquals(statusTodo.getId(), allInvocations.get(1).get(0).getPrimaryStatus().getId());
    }

    @Test
    void testCreateColumn_NullPrimaryStatusId_ThrowsBadRequest() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));

        BoardColumnCreateRequest req = new BoardColumnCreateRequest("Column", null, 0, null);

        assertThrows(BadRequestException.class,
                () -> boardService.createColumn("test-ws", "test-proj", board.getId(), req, userId));
    }

    @Test
    void testCreateColumn_CrossProjectPrimaryStatus_ThrowsBadRequest() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(statusRepository.findByIdAndProject_Id(otherProjectStatus.getId(), project.getId())).thenReturn(Optional.empty());

        BoardColumnCreateRequest req = new BoardColumnCreateRequest("Column", otherProjectStatus.getId(), 0, null);

        assertThrows(BadRequestException.class,
                () -> boardService.createColumn("test-ws", "test-proj", board.getId(), req, userId));
    }

    @Test
    void testCreateColumn_PrimaryStatusInAdditionalStatuses_ThrowsBadRequest() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(statusRepository.findByIdAndProject_Id(statusTodo.getId(), project.getId())).thenReturn(Optional.of(statusTodo));

        BoardColumnCreateRequest req = new BoardColumnCreateRequest("Column", statusTodo.getId(), 0, List.of(statusTodo.getId()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> boardService.createColumn("test-ws", "test-proj", board.getId(), req, userId));
        assertTrue(ex.getMessage().contains("Primary status cannot also be included in additional statuses"));
    }

    @Test
    void testCreateColumn_WithAdditionalStatuses_Success() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(statusRepository.findByIdAndProject_Id(statusInProgress.getId(), project.getId())).thenReturn(Optional.of(statusInProgress));
        when(statusRepository.findByIdAndProject_Id(statusDone.getId(), project.getId())).thenReturn(Optional.of(statusDone));
        when(boardColumnRepository.countByBoard_Id(board.getId())).thenReturn(1L);

        when(boardColumnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> {
            BoardColumn c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        BoardColumnCreateRequest req = new BoardColumnCreateRequest("Working", statusInProgress.getId(), null, List.of(statusDone.getId()));

        BoardColumnDto dto = boardService.createColumn("test-ws", "test-proj", board.getId(), req, userId);

        assertNotNull(dto);
        assertEquals("Working", dto.getName());
        assertEquals(statusInProgress.getId(), dto.getPrimaryStatusId());
        assertEquals(1, dto.getAdditionalStatuses().size());
        assertEquals(statusDone.getId(), dto.getAdditionalStatuses().get(0).getId());
    }

    @Test
    void testReorderColumn_MoveLeftAndRightShiftsCorrectly() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        BoardColumn col0 = new BoardColumn();
        col0.setId(UUID.randomUUID());
        col0.setBoard(board);
        col0.setPosition(0);

        BoardColumn col1 = new BoardColumn();
        col1.setId(UUID.randomUUID());
        col1.setBoard(board);
        col1.setPosition(1);

        BoardColumn col2 = new BoardColumn();
        col2.setId(UUID.randomUUID());
        col2.setBoard(board);
        col2.setPosition(2);
        col2.setPrimaryStatus(statusDone);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(boardColumnRepository.findByIdAndBoard_Id(col2.getId(), board.getId())).thenReturn(Optional.of(col2));
        when(boardColumnRepository.countByBoard_Id(board.getId())).thenReturn(3L);
        when(boardColumnRepository.findByBoardIdAndPositionBetween(board.getId(), 0, 1)).thenReturn(List.of(col0, col1));
        when(boardColumnRepository.save(col2)).thenReturn(col2);

        // Move col2 from position 2 to position 0
        boardService.reorderColumn("test-ws", "test-proj", board.getId(), col2.getId(), new ColumnReorderRequest(0), userId);

        assertEquals(0, col2.getPosition());
        assertEquals(1, col0.getPosition()); // shifted +1
        assertEquals(2, col1.getPosition()); // shifted +1
        verify(boardColumnRepository).saveAll(List.of(col0, col1));
    }

    @Test
    void testDeleteColumn_SucceedsAndShiftsSubsequentColumns() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setProject(project);

        BoardColumn col1 = new BoardColumn();
        col1.setId(UUID.randomUUID());
        col1.setBoard(board);
        col1.setPosition(1);

        BoardColumn col2 = new BoardColumn();
        col2.setId(UUID.randomUUID());
        col2.setBoard(board);
        col2.setPosition(2);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(boardColumnRepository.findByIdAndBoard_Id(col1.getId(), board.getId())).thenReturn(Optional.of(col1));
        when(boardColumnRepository.findByBoardIdAndPositionGreaterThanEqual(board.getId(), 2)).thenReturn(List.of(col2));

        boardService.deleteColumn("test-ws", "test-proj", board.getId(), col1.getId(), userId);

        verify(boardColumnRepository).delete(col1);
        assertEquals(1, col2.getPosition()); // shifted -1
        verify(boardColumnRepository).saveAll(List.of(col2));
    }

    @Test
    void testBoardAccess_UnauthorizedUserRejected() {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), nonMemberUserId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), nonMemberUserId)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> boardService.getBoards("test-ws", "test-proj", nonMemberUserId));
    }

    @Test
    void testGetBoardById_KanbanBoard_MapsAllProjectTodosIntoColumns() {
        mockSuperAdminAccess(userId);
        Board board = new KanbanBoard();
        board.setId(UUID.randomUUID());
        board.setName("Kanban Board");
        board.setProject(project);

        BoardColumn col = new BoardColumn();
        col.setId(UUID.randomUUID());
        col.setBoard(board);
        col.setName("To Do");
        col.setPosition(0);
        col.setPrimaryStatus(statusTodo);

        Todo todo1 = new Todo();
        todo1.setId(UUID.randomUUID());
        todo1.setProject(project);
        todo1.setTitle("Task 1");
        todo1.setStatus(statusTodo);

        Todo todo2 = new Todo();
        todo2.setId(UUID.randomUUID());
        todo2.setProject(project);
        todo2.setTitle("Task 2");
        todo2.setStatus(statusDone);

        when(boardRepository.findByIdAndProject_Id(board.getId(), project.getId())).thenReturn(Optional.of(board));
        when(boardColumnRepository.findByBoard_IdOrderByPositionAsc(board.getId())).thenReturn(List.of(col));
        when(todoRepository.findByProject_Id(project.getId())).thenReturn(List.of(todo1, todo2));

        BoardDto dto = boardService.getBoardById("test-ws", "test-proj", board.getId(), userId);

        assertNotNull(dto);
        assertEquals(1, dto.getColumns().size());
        assertEquals(1, dto.getColumns().get(0).getTodos().size());
        assertEquals("Task 1", dto.getColumns().get(0).getTodos().get(0).getTitle());
    }

    @Test
    void testGetBoardById_SprintBoard_FiltersBySprintId() {
        mockSuperAdminAccess(userId);
        SprintBoard sprint = new SprintBoard();
        sprint.setId(UUID.randomUUID());
        sprint.setName("Sprint 1");
        sprint.setProject(project);

        BoardColumn col = new BoardColumn();
        col.setId(UUID.randomUUID());
        col.setBoard(sprint);
        col.setName("To Do");
        col.setPosition(0);
        col.setPrimaryStatus(statusTodo);

        Todo sprintTodo = new Todo();
        sprintTodo.setId(UUID.randomUUID());
        sprintTodo.setProject(project);
        sprintTodo.setTitle("Sprint Task");
        sprintTodo.setStatus(statusTodo);
        sprintTodo.setSprint(sprint);

        when(boardRepository.findByIdAndProject_Id(sprint.getId(), project.getId())).thenReturn(Optional.of(sprint));
        when(boardColumnRepository.findByBoard_IdOrderByPositionAsc(sprint.getId())).thenReturn(List.of(col));
        when(todoRepository.findByProject_IdAndSprint_Id(project.getId(), sprint.getId())).thenReturn(List.of(sprintTodo));

        BoardDto dto = boardService.getBoardById("test-ws", "test-proj", sprint.getId(), userId);

        assertNotNull(dto);
        assertEquals(1, dto.getColumns().size());
        assertEquals(1, dto.getColumns().get(0).getTodos().size());
        assertEquals("Sprint Task", dto.getColumns().get(0).getTodos().get(0).getTitle());
        verify(todoRepository).findByProject_IdAndSprint_Id(project.getId(), sprint.getId());
    }
}
