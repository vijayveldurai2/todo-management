package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.ProjectDto;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRoleRepository projectRoleRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private StatusRepository statusRepository;

    private ProjectDto mapToDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setWorkspaceId(project.getWorkspace().getId());
        dto.setName(project.getName());
        dto.setSlug(project.getSlug());
        dto.setDescription(project.getDescription());
        dto.setPrefixCode(project.getPrefixCode());
        dto.setStatus(project.getStatus().name());
        dto.setCreatedBy(project.getCreatedBy().getId());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }

    private String generateUniqueSlug(UUID workspaceId, String name) {
        String base = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "project";
        }
        String slug = base;
        int suffix = 2;
        while (projectRepository.existsByWorkspace_IdAndSlug(workspaceId, slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    private String generatePrefixCode(String name) {
        String code = name.trim().toUpperCase().replaceAll("[^A-Z]", "");
        if (code.length() > 3) {
            code = code.substring(0, 3);
        } else if (code.length() < 2) {
            code = (code + "XX").substring(0, 2);
        }
        return code;
    }

    private void validateWorkspaceAdmin(Workspace workspace, UUID userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this workspace"));
        
        // Checking for SUPER_ADMIN. If ADMIN role is added later, check it here too.
        if (member.getRole() != WorkspaceMember.Role.SUPER_ADMIN) {
            throw new RuntimeException("Caller must be a workspace ADMIN or SUPER_ADMIN");
        }
    }

    @Override
    @Transactional
    public ProjectDto createProject(String workspaceSlug, ProjectDto dto, UUID creatorId) {
        Workspace workspace = workspaceRepository.findBySlug(workspaceSlug)
                .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceSlug));
                
        validateWorkspaceAdmin(workspace, creatorId);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorId));

        Project project = new Project();
        project.setWorkspace(workspace);
        project.setName(dto.getName());
        project.setSlug(generateUniqueSlug(workspace.getId(), dto.getName()));
        project.setDescription(dto.getDescription());
        project.setStatus(Project.Status.ACTIVE);
        
        String prefix = dto.getPrefixCode();
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = generatePrefixCode(dto.getName());
        }
        project.setPrefixCode(prefix);
        project.setCreatedBy(creator);
        
        Project savedProject = projectRepository.save(project);

        // Seed default roles
        ProjectRole adminRole = new ProjectRole();
        adminRole.setProject(savedProject);
        adminRole.setName("Admin");
        adminRole.setAdmin(true);
        adminRole = projectRoleRepository.save(adminRole);

        ProjectRole memberRole = new ProjectRole();
        memberRole.setProject(savedProject);
        memberRole.setName("Member");
        memberRole.setAdmin(false);
        projectRoleRepository.save(memberRole);

        // Assign creator as Admin
        ProjectMember projectMember = new ProjectMember();
        projectMember.setProject(savedProject);
        projectMember.setUser(creator);
        projectMember.setProjectRole(adminRole);
        projectMemberRepository.save(projectMember);

        // Seed 3 default project statuses
        Status todoStatus = new Status();
        todoStatus.setProject(savedProject);
        todoStatus.setName("To Do");
        todoStatus.setCategory(StatusCategory.NOT_STARTED);
        todoStatus.setPosition(0);
        statusRepository.save(todoStatus);

        Status inProgressStatus = new Status();
        inProgressStatus.setProject(savedProject);
        inProgressStatus.setName("In Progress");
        inProgressStatus.setCategory(StatusCategory.IN_PROGRESS);
        inProgressStatus.setPosition(1);
        statusRepository.save(inProgressStatus);

        Status doneStatus = new Status();
        doneStatus.setProject(savedProject);
        doneStatus.setName("Done");
        doneStatus.setCategory(StatusCategory.DONE);
        doneStatus.setPosition(2);
        statusRepository.save(doneStatus);

        return mapToDto(savedProject);
    }

    @Override
    public List<ProjectDto> getProjectsByWorkspace(String workspaceSlug, UUID userId) {
        Workspace workspace = workspaceRepository.findBySlug(workspaceSlug)
                .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceSlug));
                
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        List<Project> projects = projectRepository.findByWorkspace_Slug(workspaceSlug);
        
        if (!isSuperAdmin) {
            projects = projects.stream()
                    .filter(p -> projectMemberRepository.existsByProject_IdAndUser_Id(p.getId(), userId))
                    .collect(Collectors.toList());
        }

        return projects.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDto getProjectBySlug(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectSlug));
                
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);
                
        if (!isSuperAdmin && !projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId)) {
            throw new RuntimeException("Access denied");
        }
        
        return mapToDto(project);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(String workspaceSlug, String projectSlug, ProjectDto dto, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectSlug));
                
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);
                
        boolean isProjectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .map(pm -> pm.getProjectRole().isAdmin())
                .orElse(false);
                
        if (!isSuperAdmin && !isProjectAdmin) {
             throw new RuntimeException("Caller must be a project Admin or workspace SUPER_ADMIN");
        }
        
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            project.setStatus(Project.Status.valueOf(dto.getStatus()));
        }
        
        return mapToDto(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void archiveProject(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectSlug));
        
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);
                
        boolean isProjectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .map(pm -> pm.getProjectRole().isAdmin())
                .orElse(false);
                
        if (!isSuperAdmin && !isProjectAdmin) {
             throw new RuntimeException("Caller must be a project Admin or workspace SUPER_ADMIN");
        }
        
        project.setStatus(Project.Status.ARCHIVED);
        projectRepository.save(project);
    }
}
