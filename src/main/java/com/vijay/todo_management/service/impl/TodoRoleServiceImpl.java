package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.TodoRoleCreateRequest;
import com.vijay.todo_management.dto.TodoRoleDto;
import com.vijay.todo_management.dto.TodoRoleUpdateRequest;
import com.vijay.todo_management.entity.Project;
import com.vijay.todo_management.entity.TodoRole;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.ProjectMemberRepository;
import com.vijay.todo_management.repository.ProjectRepository;
import com.vijay.todo_management.repository.TodoAssignmentRepository;
import com.vijay.todo_management.repository.TodoRoleRepository;
import com.vijay.todo_management.repository.WorkspaceMemberRepository;
import com.vijay.todo_management.service.TodoRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TodoRoleServiceImpl implements TodoRoleService {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private TodoRoleRepository todoRoleRepository;
    @Autowired private TodoAssignmentRepository todoAssignmentRepository;

    // ── Authorization helper (mirrors TodoServiceImpl pattern) ──────────────

    private Project getProjectAndValidateAccess(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectSlug + " in workspace: " + workspaceSlug));

        boolean isSuperAdmin = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId);

        if (!isSuperAdmin && !isProjectMember) {
            throw new ForbiddenException("Access denied: caller is not a member of this project or workspace");
        }
        return project;
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private TodoRoleDto mapToDto(TodoRole role) {
        TodoRoleDto dto = new TodoRoleDto();
        dto.setId(role.getId());
        dto.setProjectId(role.getProject() != null ? role.getProject().getId() : null);
        dto.setName(role.getName());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    // ── Service Methods ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TodoRoleDto> listRoles(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        return todoRoleRepository.findByProject_IdOrderByNameAsc(project.getId())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TodoRoleDto createRole(String workspaceSlug, String projectSlug, TodoRoleCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Role name is required");
        }

        if (todoRoleRepository.existsByProject_IdAndNameIgnoreCase(project.getId(), request.getName().trim())) {
            throw new ResourceConflictException("A todo role named '" + request.getName().trim() + "' already exists in this project");
        }

        TodoRole role = new TodoRole();
        role.setProject(project);
        role.setName(request.getName().trim());
        return mapToDto(todoRoleRepository.save(role));
    }

    @Override
    @Transactional
    public TodoRoleDto updateRole(String workspaceSlug, String projectSlug, UUID roleId, TodoRoleUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        TodoRole role = todoRoleRepository.findByIdAndProject_Id(roleId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TodoRole not found: " + roleId));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName())
                    && todoRoleRepository.existsByProject_IdAndNameIgnoreCase(project.getId(), newName)) {
                throw new ResourceConflictException("A todo role named '" + newName + "' already exists in this project");
            }
            role.setName(newName);
        }

        return mapToDto(todoRoleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(String workspaceSlug, String projectSlug, UUID roleId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        TodoRole role = todoRoleRepository.findByIdAndProject_Id(roleId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TodoRole not found: " + roleId));

        // Guard: refuse deletion if any assignment still references this role
        if (todoAssignmentRepository.existsByTodoRole_Id(role.getId())) {
            throw new ResourceConflictException(
                    "Cannot delete TodoRole '" + role.getName() + "': it is still referenced by one or more assignments. "
                    + "Reassign or remove those assignments first.");
        }

        todoRoleRepository.delete(role);
    }
}
