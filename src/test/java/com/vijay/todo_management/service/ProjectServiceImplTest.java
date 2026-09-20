package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ProjectDto;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private StatusRepository statusRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID userId;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setSlug("test-ws");

        user = new User();
        user.setId(userId);
        user.setName("Test User");
    }

    @Test
    void testCreateProject_AutoSeedsThreeDefaultStatuses() {
        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(WorkspaceMember.Role.SUPER_ADMIN);

        when(workspaceRepository.findBySlug("test-ws")).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)).thenReturn(Optional.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(projectRepository.existsByWorkspace_IdAndSlug(any(), any())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(projectRoleRepository.save(any(ProjectRole.class))).thenAnswer(inv -> {
            ProjectRole r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ProjectDto dto = new ProjectDto();
        dto.setName("Project Alpha");
        dto.setDescription("Testing status seeding");

        ProjectDto created = projectService.createProject("test-ws", dto, userId);

        assertNotNull(created);
        assertEquals("Project Alpha", created.getName());

        // Verify that 3 default statuses were saved
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(statusRepository, times(3)).save(statusCaptor.capture());

        List<Status> savedStatuses = statusCaptor.getAllValues();
        assertEquals(3, savedStatuses.size());

        Status status0 = savedStatuses.get(0);
        assertEquals("To Do", status0.getName());
        assertEquals(StatusCategory.NOT_STARTED, status0.getCategory());
        assertEquals(0, status0.getPosition());

        Status status1 = savedStatuses.get(1);
        assertEquals("In Progress", status1.getName());
        assertEquals(StatusCategory.IN_PROGRESS, status1.getCategory());
        assertEquals(1, status1.getPosition());

        Status status2 = savedStatuses.get(2);
        assertEquals("Done", status2.getName());
        assertEquals(StatusCategory.DONE, status2.getCategory());
        assertEquals(2, status2.getPosition());
    }
}
