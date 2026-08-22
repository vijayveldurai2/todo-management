package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.WorkspaceInviteDto;
import com.vijay.todo_management.dto.WorkspaceInviteRequestDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;

import java.util.List;
import java.util.UUID;

public interface WorkspaceInviteService {
    WorkspaceInviteDto createInvite(UUID workspaceId, UUID inviterId, WorkspaceInviteRequestDto req);
    List<WorkspaceInviteDto> getPendingInvites(UUID workspaceId);
    List<WorkspaceInviteDto> getMyPendingInvites(UUID userId);
    WorkspaceMemberDto acceptInvite(UUID inviteId, UUID acceptingUserId);
    void declineInvite(UUID inviteId, UUID decliningUserId);
    void revokeInvite(UUID inviteId);

    WorkspaceMemberDto acceptInviteByToken(String rawToken, UUID acceptingUserId);
    void declineInviteByToken(String rawToken);
}
