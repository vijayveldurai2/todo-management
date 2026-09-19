package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.ChecklistItemDto;
import com.vijay.todo_management.dto.ChecklistItemUpdateRequest;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.ChecklistItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistItemServiceImplTest {

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ChecklistItemServiceImpl checklistItemService;

    private UUID userId;
    private UUID nonMemberUserId;
    private Workspace workspace;
    private Project project;
    private Todo todo;
    private ChecklistItem item0;
    private ChecklistItem item1;
    private ChecklistItem item2;

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

        todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setProject(project);

        item0 = new ChecklistItem();
        item0.setId(UUID.randomUUID());
        item0.setTodo(todo);
        item0.setText("Item 0");
        item0.setChecked(false);
        item0.setPosition(0);

        item1 = new ChecklistItem();
        item1.setId(UUID.randomUUID());
        item1.setTodo(todo);
        item1.setText("Item 1");
        item1.setChecked(true);
        item1.setPosition(1);

        item2 = new ChecklistItem();
        item2.setId(UUID.randomUUID());
        item2.setTodo(todo);
        item2.setText("Item 2");
        item2.setChecked(false);
        item2.setPosition(2);
    }

    private void mockProjectAccess(UUID callerId) {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), callerId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), callerId)).thenReturn(true);
    }

    @Test
    void getChecklistItems_validProjectMember_returnsSortedByPositionAsc() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByTodo_IdOrderByPositionAsc(todo.getId())).thenReturn(List.of(item0, item1, item2));

        List<ChecklistItemDto> result = checklistItemService.getChecklistItems("test-ws", "test-proj", todo.getId(), userId);

        assertEquals(3, result.size());
        assertEquals("Item 0", result.get(0).getText());
        assertEquals(0, result.get(0).getPosition());
        assertEquals("Item 1", result.get(1).getText());
        assertTrue(result.get(1).isChecked());
        assertEquals("Item 2", result.get(2).getText());
    }

    @Test
    void getChecklistItems_nonMember_throwsForbiddenException() {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), nonMemberUserId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), nonMemberUserId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                checklistItemService.getChecklistItems("test-ws", "test-proj", todo.getId(), nonMemberUserId));
    }

    @Test
    void getChecklistItems_todoNotFound_throwsResourceNotFoundException() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                checklistItemService.getChecklistItems("test-ws", "test-proj", todo.getId(), userId));
    }

    @Test
    void addItem_withoutPosition_appendsAtEndOfList() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.countByTodo_Id(todo.getId())).thenReturn(3);

        when(checklistItemRepository.save(any(ChecklistItem.class))).thenAnswer(inv -> {
            ChecklistItem saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ChecklistItemCreateRequest request = new ChecklistItemCreateRequest("New Item", null);
        ChecklistItemDto result = checklistItemService.addItem("test-ws", "test-proj", todo.getId(), request, userId);

        assertNotNull(result);
        assertEquals("New Item", result.getText());
        assertEquals(3, result.getPosition());
        assertFalse(result.isChecked());
        verify(checklistItemRepository, never()).findByTodoIdAndPositionGreaterThanEqual(any(), anyInt());
    }

    @Test
    void addItem_withCustomPosition_shiftsExistingPositionsAndInserts() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.countByTodo_Id(todo.getId())).thenReturn(3);
        when(checklistItemRepository.findByTodoIdAndPositionGreaterThanEqual(todo.getId(), 1))
                .thenReturn(List.of(item1, item2));

        when(checklistItemRepository.save(any(ChecklistItem.class))).thenAnswer(inv -> {
            ChecklistItem saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ChecklistItemCreateRequest request = new ChecklistItemCreateRequest("Inserted Item", 1);
        ChecklistItemDto result = checklistItemService.addItem("test-ws", "test-proj", todo.getId(), request, userId);

        assertNotNull(result);
        assertEquals("Inserted Item", result.getText());
        assertEquals(1, result.getPosition());

        // Check that item1 and item2 positions were shifted
        assertEquals(2, item1.getPosition());
        assertEquals(3, item2.getPosition());
        verify(checklistItemRepository).saveAll(List.of(item1, item2));
    }

    @Test
    void addItem_blankText_throwsBadRequestException() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));

        ChecklistItemCreateRequest request = new ChecklistItemCreateRequest("   ", 0);
        assertThrows(BadRequestException.class, () ->
                checklistItemService.addItem("test-ws", "test-proj", todo.getId(), request, userId));
    }

    @Test
    void updateItem_toggleIsChecked_updatesCheckedState() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item0.getId(), todo.getId())).thenReturn(Optional.of(item0));
        when(checklistItemRepository.save(any(ChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemUpdateRequest request = new ChecklistItemUpdateRequest(null, true);
        ChecklistItemDto result = checklistItemService.updateItem("test-ws", "test-proj", todo.getId(), item0.getId(), request, userId);

        assertTrue(result.isChecked());
        assertEquals("Item 0", result.getText());
    }

    @Test
    void updateItem_updateText_updatesText() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item0.getId(), todo.getId())).thenReturn(Optional.of(item0));
        when(checklistItemRepository.save(any(ChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemUpdateRequest request = new ChecklistItemUpdateRequest("Updated Title", null);
        ChecklistItemDto result = checklistItemService.updateItem("test-ws", "test-proj", todo.getId(), item0.getId(), request, userId);

        assertEquals("Updated Title", result.getText());
    }

    @Test
    void updateItem_emptyText_throwsBadRequestException() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item0.getId(), todo.getId())).thenReturn(Optional.of(item0));

        ChecklistItemUpdateRequest request = new ChecklistItemUpdateRequest("   ", null);
        assertThrows(BadRequestException.class, () ->
                checklistItemService.updateItem("test-ws", "test-proj", todo.getId(), item0.getId(), request, userId));
    }

    @Test
    void reorderItem_moveDown_shiftsIntermediateItemsUpAndUpdatesTarget() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item0.getId(), todo.getId())).thenReturn(Optional.of(item0));
        when(checklistItemRepository.countByTodo_Id(todo.getId())).thenReturn(3);
        // moving item0 from pos 0 to pos 2: items between (0+1=1 to 2) shift down by 1
        when(checklistItemRepository.findByTodoIdAndPositionBetween(todo.getId(), 1, 2))
                .thenReturn(List.of(item1, item2));

        ChecklistItemDto result = checklistItemService.reorderItem("test-ws", "test-proj", todo.getId(), item0.getId(), 2, userId);

        assertEquals(2, result.getPosition());
        assertEquals(0, item1.getPosition());
        assertEquals(1, item2.getPosition());
        verify(checklistItemRepository).saveAll(List.of(item1, item2));
        verify(checklistItemRepository).save(item0);
    }

    @Test
    void reorderItem_moveUp_shiftsIntermediateItemsDownAndUpdatesTarget() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item2.getId(), todo.getId())).thenReturn(Optional.of(item2));
        when(checklistItemRepository.countByTodo_Id(todo.getId())).thenReturn(3);
        // moving item2 from pos 2 to pos 0: items between (0 to 2-1=1) shift up by 1
        when(checklistItemRepository.findByTodoIdAndPositionBetween(todo.getId(), 0, 1))
                .thenReturn(List.of(item0, item1));

        ChecklistItemDto result = checklistItemService.reorderItem("test-ws", "test-proj", todo.getId(), item2.getId(), 0, userId);

        assertEquals(0, result.getPosition());
        assertEquals(1, item0.getPosition());
        assertEquals(2, item1.getPosition());
        verify(checklistItemRepository).saveAll(List.of(item0, item1));
        verify(checklistItemRepository).save(item2);
    }

    @Test
    void reorderItem_outOfBounds_clampsToMaxPosition() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item0.getId(), todo.getId())).thenReturn(Optional.of(item0));
        when(checklistItemRepository.countByTodo_Id(todo.getId())).thenReturn(3);
        // Requesting 99 on a list of 3 items clamps to 2 (currentCount - 1)
        when(checklistItemRepository.findByTodoIdAndPositionBetween(todo.getId(), 1, 2))
                .thenReturn(List.of(item1, item2));

        ChecklistItemDto result = checklistItemService.reorderItem("test-ws", "test-proj", todo.getId(), item0.getId(), 99, userId);

        assertEquals(2, result.getPosition());
    }

    @Test
    void deleteItem_removesItemAndShiftsSubsequentPositionsDown() {
        mockProjectAccess(userId);
        when(todoRepository.findByIdAndProject_Id(todo.getId(), project.getId())).thenReturn(Optional.of(todo));
        when(checklistItemRepository.findByIdAndTodo_Id(item1.getId(), todo.getId())).thenReturn(Optional.of(item1));
        when(checklistItemRepository.findByTodoIdAndPositionGreaterThanEqual(todo.getId(), 2))
                .thenReturn(List.of(item2));

        checklistItemService.deleteItem("test-ws", "test-proj", todo.getId(), item1.getId(), userId);

        verify(checklistItemRepository).delete(item1);
        assertEquals(1, item2.getPosition()); // item2 shifted from 2 to 1
        verify(checklistItemRepository).saveAll(List.of(item2));
    }
}
