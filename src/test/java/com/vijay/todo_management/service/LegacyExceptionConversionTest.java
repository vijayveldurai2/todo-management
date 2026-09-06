package com.vijay.todo_management.service;

import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.ProjectMemberServiceImpl;
import com.vijay.todo_management.service.impl.ProjectServiceImpl;
import com.vijay.todo_management.service.impl.WorkspaceInviteServiceImpl;
import com.vijay.todo_management.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyExceptionConversionTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectRoleRepository projectRoleRepository;
    @Mock
    private WorkspaceInviteRepository workspaceInviteRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    @InjectMocks
    private WorkspaceInviteServiceImpl workspaceInviteService;

    private UUID userId;
    private Workspace workspace;
    private Project project;
    private User callerUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setSlug("test-ws");

        callerUser = new User();
        callerUser.setId(userId);
        callerUser.setEmail("caller@example.com");

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setSlug("test-proj");
        project.setWorkspace(workspace);
    }

    @Test
    void testWorkspaceService_AddMember_DuplicateMember_ThrowsResourceConflictException() {
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(userRepository.findById(userId)).thenReturn(Optional.of(callerUser));
        when(workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspace.getId(), userId)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () ->
                workspaceService.addMember(workspace.getId(), userId)
        );

        assertTrue(ex.getMessage().contains("User is already a member of this workspace"));
    }

    @Test
    void testProjectService_GetProjectBySlug_UnauthorizedUser_ThrowsForbiddenExceptionWithDescriptiveMessage() {
        when(projectRepository.findByWorkspace_SlugAndSlug("test-ws", "test-proj")).thenReturn(Optional.of(project));
        // User is not in workspace
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)).thenReturn(Optional.empty());
        // User is not in project
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                projectService.getProjectBySlug("test-ws", "test-proj", userId)
        );

        assertEquals("Access denied: caller is not a member of this project or workspace", ex.getMessage());
    }

    @Test
    void testProjectMemberService_AddMember_NonAdminCaller_ThrowsForbiddenException() {
        when(projectRepository.findFirstBySlug("test-proj")).thenReturn(Optional.of(project));
        // Caller has no SUPER_ADMIN workspace role
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)).thenReturn(Optional.empty());
        // Caller has no admin project role
        when(projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)).thenReturn(Optional.empty());

        UUID targetUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                projectMemberService.addMember("test-proj", targetUserId, roleId, userId)
        );

        assertTrue(ex.getMessage().contains("Caller must be a project Admin or workspace SUPER_ADMIN"));
    }

    @Test
    void testProjectMemberService_AddMember_DuplicateMember_ThrowsResourceConflictException() {
        when(projectRepository.findFirstBySlug("test-proj")).thenReturn(Optional.of(project));

        // Caller is SUPER_ADMIN
        WorkspaceMember wsAdmin = new WorkspaceMember();
        wsAdmin.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)).thenReturn(Optional.of(wsAdmin));

        UUID targetUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        // Target user is workspace member
        when(workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspace.getId(), targetUserId)).thenReturn(true);
        // But already a project member
        when(projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), targetUserId)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () ->
                projectMemberService.addMember("test-proj", targetUserId, roleId, userId)
        );

        assertTrue(ex.getMessage().contains("User is already a member of this project"));
    }

    @Test
    void testWorkspaceInviteService_AcceptInvite_MismatchedEmail_ThrowsBadRequestException() {
        UUID inviteId = UUID.randomUUID();
        WorkspaceInvite invite = new WorkspaceInvite();
        invite.setId(inviteId);
        invite.setWorkspace(workspace);
        invite.setEmail("intended@example.com");
        invite.setStatus(WorkspaceInvite.Status.PENDING);
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(workspaceInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(userRepository.findById(userId)).thenReturn(Optional.of(callerUser)); // caller email is "caller@example.com"

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                workspaceInviteService.acceptInvite(inviteId, userId)
        );

        assertTrue(ex.getMessage().contains("User email does not match invite email"));
    }
}
