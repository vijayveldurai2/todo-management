package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.ProjectRoleDto;
import com.vijay.todo_management.entity.Project;
import com.vijay.todo_management.entity.ProjectRole;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.repository.ProjectMemberRepository;
import com.vijay.todo_management.repository.ProjectRepository;
import com.vijay.todo_management.repository.ProjectRoleRepository;
import com.vijay.todo_management.repository.WorkspaceMemberRepository;
import com.vijay.todo_management.service.ProjectRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectRoleServiceImpl implements ProjectRoleService {

    @Autowired
    private ProjectRoleRepository projectRoleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    private ProjectRoleDto mapToDto(ProjectRole role) {
        ProjectRoleDto dto = new ProjectRoleDto();
        dto.setId(role.getId());
        dto.setProjectId(role.getProject().getId());
        dto.setName(role.getName());
        dto.setAdmin(role.isAdmin());
        dto.setCreatedAt(role.getCreatedAt());
        return dto;
    }

    private Project getProjectBySlug(String slug) {
        return projectRepository.findFirstBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Project not found: " + slug));
    }

    private void validateProjectAdmin(Project project, UUID userId) {
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);
                
        if (isSuperAdmin) return;
        
        boolean isProjectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .map(pm -> pm.getProjectRole().isAdmin())
                .orElse(false);
                
        if (!isProjectAdmin) {
            throw new RuntimeException("Caller must be a project Admin or workspace SUPER_ADMIN");
        }
    }

    @Override
    public List<ProjectRoleDto> getRoles(String projectSlug, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        // Any workspace member can view roles? The prompt didn't specify. Assuming yes, or admin only. 
        // Let's just return it for now.
        return projectRoleRepository.findByProject_Slug(projectSlug).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectRoleDto createRole(String projectSlug, ProjectRoleDto dto, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        if (projectRoleRepository.existsByProject_IdAndName(project.getId(), dto.getName())) {
            throw new RuntimeException("Role already exists with name: " + dto.getName());
        }

        ProjectRole role = new ProjectRole();
        role.setProject(project);
        role.setName(dto.getName());
        role.setAdmin(dto.isAdmin());
        return mapToDto(projectRoleRepository.save(role));
    }

    @Override
    @Transactional
    public ProjectRoleDto updateRole(String projectSlug, UUID roleId, ProjectRoleDto dto, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        ProjectRole role = projectRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
                
        if (!role.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Role does not belong to this project");
        }

        if (!role.getName().equals(dto.getName()) && projectRoleRepository.existsByProject_IdAndName(project.getId(), dto.getName())) {
            throw new RuntimeException("Role already exists with name: " + dto.getName());
        }

        role.setName(dto.getName());
        
        // If unsetting admin, ensure it's not the last one
        if (role.isAdmin() && !dto.isAdmin()) {
            long adminCount = projectRoleRepository.countByProject_IdAndIsAdminTrue(project.getId());
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot unset admin on the last admin role for this project");
            }
        }
        role.setAdmin(dto.isAdmin());

        return mapToDto(projectRoleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(String projectSlug, UUID roleId, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        ProjectRole role = projectRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        if (!role.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Role does not belong to this project");
        }

        // Cannot delete last admin role
        if (role.isAdmin()) {
            long adminCount = projectRoleRepository.countByProject_IdAndIsAdminTrue(project.getId());
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot delete the last admin role for this project");
            }
        }

        // Block if members currently hold it
        long membersWithRole = projectMemberRepository.countByProjectRole_Id(roleId);
        if (membersWithRole > 0) {
            throw new RuntimeException("Cannot delete role: " + membersWithRole + " members currently hold it");
        }

        projectRoleRepository.delete(role);
    }
}
