package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.WorkspaceDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
    WorkspaceDto createWorkspace(WorkspaceDto dto, UUID creatorId);
    List<WorkspaceDto> getWorkspacesForUser(UUID userId);
    WorkspaceDto getWorkspaceById(UUID workspaceId);
    WorkspaceDto getWorkspaceBySlug(String slug);
    WorkspaceDto updateWorkspace(UUID workspaceId, WorkspaceDto dto);
    void deleteWorkspace(UUID workspaceId);

    List<WorkspaceMemberDto> getMembers(UUID workspaceId);
    WorkspaceMemberDto addMember(UUID workspaceId, UUID userId);
    WorkspaceMemberDto changeMemberRole(UUID workspaceId, UUID userId, String role);
    void removeMember(UUID workspaceId, UUID userId);
}
