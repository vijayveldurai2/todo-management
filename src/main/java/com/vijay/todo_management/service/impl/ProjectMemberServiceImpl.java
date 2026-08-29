package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.ProjectMemberDto;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.ProjectMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectRoleRepository projectRoleRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    
    @Autowired
    private UserRepository userRepository;

    private ProjectMemberDto mapToDto(ProjectMember member) {
        ProjectMemberDto dto = new ProjectMemberDto();
        dto.setId(member.getId());
        dto.setProjectId(member.getProject().getId());
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getName());
        dto.setUserEmail(member.getUser().getEmail());
        dto.setRoleId(member.getRole().getId());
        dto.setRoleName(member.getRole().getName());
        dto.setJoinedAt(member.getJoinedAt());
        dto.setCreatedAt(member.getCreatedAt());
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
                .map(pm -> pm.getRole().isAdmin())
                .orElse(false);
                
        if (!isProjectAdmin) {
            throw new RuntimeException("Caller must be a project Admin or workspace SUPER_ADMIN");
        }
    }

    @Override
    public List<ProjectMemberDto> getMembers(String projectSlug, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), currentUserId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);
                
        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), currentUserId);
        
        if (!isSuperAdmin && !isProjectMember) {
            throw new RuntimeException("Access denied: You must be a project member to view the member list.");
        }
        
        return projectMemberRepository.findByProject_Slug(projectSlug).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectMemberDto addMember(String projectSlug, UUID userId, UUID roleId, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        // Validate target is a workspace member
        boolean isWorkspaceMember = workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId);
        if (!isWorkspaceMember) {
            throw new RuntimeException("User must be a member of the workspace before being added to a project");
        }
        
        if (projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId)) {
            throw new RuntimeException("User is already a member of this project");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        ProjectRole role;
        if (roleId != null) {
            role = projectRoleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
            if (!role.getProject().getId().equals(project.getId())) {
                throw new RuntimeException("Role does not belong to this project");
            }
        } else {
            // Default to Member role
            role = projectRoleRepository.findByProject_Slug(projectSlug).stream()
                    .filter(r -> !r.isAdmin() && "Member".equalsIgnoreCase(r.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Default Member role not found"));
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        
        return mapToDto(projectMemberRepository.save(member));
    }

    @Override
    @Transactional
    public ProjectMemberDto changeMemberRole(String projectSlug, UUID userId, UUID roleId, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        ProjectMember member = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Project membership not found for user: " + userId));

        ProjectRole newRole = projectRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
                
        if (!newRole.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Role does not belong to this project");
        }

        member.setRole(newRole);
        return mapToDto(projectMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(String projectSlug, UUID userId, UUID currentUserId) {
        Project project = getProjectBySlug(projectSlug);
        validateProjectAdmin(project, currentUserId);

        ProjectMember member = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Project membership not found for user: " + userId));

        projectMemberRepository.delete(member);
    }
}
