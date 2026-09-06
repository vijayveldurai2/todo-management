package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.WorkspaceDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.entity.Workspace;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.repository.WorkspaceMemberRepository;
import com.vijay.todo_management.repository.WorkspaceRepository;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------

    private WorkspaceDto mapToDto(Workspace workspace) {
        WorkspaceDto dto = new WorkspaceDto();
        dto.setId(workspace.getId());
        dto.setName(workspace.getName());
        dto.setSlug(workspace.getSlug());
        dto.setDescription(workspace.getDescription());
        dto.setCreatedBy(workspace.getCreatedBy().getId());
        dto.setCreatedAt(workspace.getCreatedAt());
        dto.setUpdatedAt(workspace.getUpdatedAt());
        return dto;
    }

    private WorkspaceMemberDto mapMemberToDto(WorkspaceMember member) {
        WorkspaceMemberDto dto = new WorkspaceMemberDto();
        dto.setId(member.getId());
        dto.setWorkspaceId(member.getWorkspace().getId());
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getName());
        dto.setUserEmail(member.getUser().getEmail());
        dto.setRole(member.getRole().name());
        dto.setStatus(member.getStatus().name());
        dto.setCreatedAt(member.getCreatedAt());
        return dto;
    }

    private String generateUniqueSlug(String name) {
        String base = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "workspace";
        }
        String slug = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    // ------------------------------------------------------------------
    // Workspace CRUD
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public WorkspaceDto createWorkspace(WorkspaceDto dto, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + creatorId));

        Workspace workspace = new Workspace();
        workspace.setName(dto.getName());
        workspace.setDescription(dto.getDescription());
        workspace.setSlug(generateUniqueSlug(dto.getName()));
        workspace.setCreatedBy(creator);

        Workspace saved = workspaceRepository.save(workspace);

        // Creator automatically becomes SUPER_ADMIN — same transaction so a workspace
        // can never exist without its SA membership row.
        WorkspaceMember saMembership = new WorkspaceMember();
        saMembership.setWorkspace(saved);
        saMembership.setUser(creator);
        saMembership.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        saMembership.setStatus(WorkspaceMember.Status.ACTIVE);
        workspaceMemberRepository.save(saMembership);

        return mapToDto(saved);
    }

    @Override
    public List<WorkspaceDto> getWorkspacesForUser(UUID userId) {
        return workspaceMemberRepository.findByUser_Id(userId).stream()
                .map(WorkspaceMember::getWorkspace)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceDto getWorkspaceById(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        return mapToDto(workspace);
    }

    @Override
    public WorkspaceDto getWorkspaceBySlug(String slug) {
        Workspace workspace = workspaceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with slug: " + slug));
        return mapToDto(workspace);
    }

    @Override
    @Transactional
    public WorkspaceDto updateWorkspace(UUID workspaceId, WorkspaceDto dto) {
        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        // slug is intentionally NOT updated here — slugs are immutable once set,
        // since URLs and any external links depend on them.
        return mapToDto(workspaceRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        Workspace existing = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        // TODO: once auth is wired, only SUPER_ADMIN should be permitted to call this.
        workspaceRepository.delete(existing);
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------

    @Override
    public List<WorkspaceMemberDto> getMembers(UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspace_Id(workspaceId).stream()
                .map(this::mapMemberToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkspaceMemberDto addMember(UUID workspaceId, UUID userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, userId)) {
            throw new ResourceConflictException("User is already a member of this workspace");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceMember.Role.USER); // default role — role changes go through the dedicated endpoint
        member.setStatus(WorkspaceMember.Status.ACTIVE);

        return mapMemberToDto(workspaceMemberRepository.save(member));
    }

    @Override
    @Transactional
    public WorkspaceMemberDto changeMemberRole(UUID workspaceId, UUID userId, String role) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user " + userId + " in workspace " + workspaceId));

        WorkspaceMember.Role newRole = WorkspaceMember.Role.valueOf(role.toUpperCase());

        // TODO: once auth is wired, prevent demoting the last remaining SUPER_ADMIN
        // in a workspace — currently unguarded.
        member.setRole(newRole);
        return mapMemberToDto(workspaceMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(UUID workspaceId, UUID userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user " + userId + " in workspace " + workspaceId));
        // TODO: once auth is wired, prevent removing the last remaining SUPER_ADMIN.
        workspaceMemberRepository.delete(member);
    }
}
