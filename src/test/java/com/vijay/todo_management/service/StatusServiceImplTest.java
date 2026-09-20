package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.StatusCreateRequest;
import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.dto.StatusUpdateRequest;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.StatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusServiceImplTest {

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @InjectMocks
    private StatusServiceImpl statusService;

    private UUID userId;
    private UUID nonMemberUserId;
    private Workspace workspace;
    private Project project;
    private Status statusTodo;
    private Status statusInProgress;
    private Status statusDone;

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
    }

    private void mockSuperAdminAccess(UUID uid) {
        WorkspaceMember wm = new WorkspaceMember();
        wm.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), uid)).thenReturn(Optional.of(wm));
    }

    @Test
    void testGetStatuses_Success() {
        mockSuperAdminAccess(userId);
        when(statusRepository.findByProject_IdOrderByPositionAsc(project.getId()))
                .thenReturn(List.of(statusTodo, statusInProgress, statusDone));

        List<StatusDto> statuses = statusService.getStatuses("test-ws", "test-proj", userId);

        assertEquals(3, statuses.size());
        assertEquals("To Do", statuses.get(0).getName());
        assertEquals("In Progress", statuses.get(1).getName());
        assertEquals("Done", statuses.get(2).getName());
    }

    @Test
    void testGetStatuses_NonMemberRejected_ThrowsForbidden() {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), nonMemberUserId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), nonMemberUserId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> statusService.getStatuses("test-ws", "test-proj", nonMemberUserId));
    }

    @Test
    void testCreateStatus_Success() {
        mockSuperAdminAccess(userId);
        when(statusRepository.existsByProject_IdAndNameIgnoreCase(project.getId(), "In Review")).thenReturn(false);
        when(statusRepository.countByProject_Id(project.getId())).thenReturn(3L);
        when(statusRepository.save(any(Status.class))).thenAnswer(inv -> {
            Status s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        StatusCreateRequest req = new StatusCreateRequest("In Review", StatusCategory.IN_PROGRESS, null);
        StatusDto created = statusService.createStatus("test-ws", "test-proj", req, userId);

        assertNotNull(created);
        assertEquals("In Review", created.getName());
        assertEquals(StatusCategory.IN_PROGRESS, created.getCategory());
        assertEquals(3, created.getPosition());
    }

    @Test
    void testCreateStatus_DuplicateName_ThrowsConflict() {
        mockSuperAdminAccess(userId);
        when(statusRepository.existsByProject_IdAndNameIgnoreCase(project.getId(), "To Do")).thenReturn(true);

        StatusCreateRequest req = new StatusCreateRequest("To Do", StatusCategory.NOT_STARTED, null);
        assertThrows(ResourceConflictException.class, () -> statusService.createStatus("test-ws", "test-proj", req, userId));
    }

    @Test
    void testDeleteStatus_ReferencedAsPrimaryInColumn_ThrowsConflict() {
        mockSuperAdminAccess(userId);
        when(statusRepository.findByIdAndProject_Id(statusTodo.getId(), project.getId())).thenReturn(Optional.of(statusTodo));
        when(boardColumnRepository.existsByPrimaryStatus_Id(statusTodo.getId())).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> statusService.deleteStatus("test-ws", "test-proj", statusTodo.getId(), userId));

        assertTrue(ex.getMessage().contains("primary status"));
        verify(statusRepository, never()).delete(any());
    }

    @Test
    void testDeleteStatus_ReferencedInAdditionalStatuses_ThrowsConflict() {
        mockSuperAdminAccess(userId);
        when(statusRepository.findByIdAndProject_Id(statusInProgress.getId(), project.getId())).thenReturn(Optional.of(statusInProgress));
        when(boardColumnRepository.existsByPrimaryStatus_Id(statusInProgress.getId())).thenReturn(false);
        when(boardColumnRepository.existsByAdditionalStatuses_Id(statusInProgress.getId())).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> statusService.deleteStatus("test-ws", "test-proj", statusInProgress.getId(), userId));

        assertTrue(ex.getMessage().contains("additional grouped statuses"));
        verify(statusRepository, never()).delete(any());
    }

    @Test
    void testDeleteStatus_Unreferenced_SuccessAndReindexes() {
        mockSuperAdminAccess(userId);
        Status customStatus = new Status();
        customStatus.setId(UUID.randomUUID());
        customStatus.setProject(project);
        customStatus.setName("Blocked");
        customStatus.setCategory(StatusCategory.IN_PROGRESS);
        customStatus.setPosition(1);

        Status subsequentStatus = new Status();
        subsequentStatus.setId(UUID.randomUUID());
        subsequentStatus.setProject(project);
        subsequentStatus.setName("Done");
        subsequentStatus.setPosition(2);

        when(statusRepository.findByIdAndProject_Id(customStatus.getId(), project.getId())).thenReturn(Optional.of(customStatus));
        when(boardColumnRepository.existsByPrimaryStatus_Id(customStatus.getId())).thenReturn(false);
        when(boardColumnRepository.existsByAdditionalStatuses_Id(customStatus.getId())).thenReturn(false);
        when(statusRepository.findByProjectIdAndPositionGreaterThanEqual(project.getId(), 2))
                .thenReturn(List.of(subsequentStatus));

        statusService.deleteStatus("test-ws", "test-proj", customStatus.getId(), userId);

        verify(statusRepository).delete(customStatus);
        assertEquals(1, subsequentStatus.getPosition()); // shifted -1
        verify(statusRepository).saveAll(List.of(subsequentStatus));
    }
}
