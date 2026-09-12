package com.vijay.todo_management.service;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AttachmentSettingsService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    @Value("${app.attachments.default-max-bytes:10485760}") private long defaultMax = 10485760;
    @Value("${app.attachments.max-bytes:26214400}") private long appMax = 26214400;
    public record Settings(long maxFileBytes, long applicationMaxFileBytes) {}
    private WorkspaceMember membership(UUID workspaceId, UUID user) {
        return members.findByWorkspace_IdAndUser_Id(workspaceId, user)
                .orElseThrow(() -> new ForbiddenException("Workspace membership required"));
    }
    @Transactional(readOnly = true)
    public Settings get(UUID workspaceId, UUID user) {
        membership(workspaceId, user);
        var ws = workspaces.findById(workspaceId).orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return new Settings(Math.min(appMax, ws.getAttachmentMaxBytes() == null ? defaultMax : ws.getAttachmentMaxBytes()),appMax);
    }
    @Transactional
    public Settings update(UUID workspaceId, UUID user, Long limit) {
        if (membership(workspaceId, user).getRole() != WorkspaceMember.Role.SUPER_ADMIN)
            throw new ForbiddenException("Workspace SUPER_ADMIN required");
        if (limit == null || limit < 1 || limit > appMax)
            throw new BadRequestException("maxFileBytes must be between 1 and " + appMax);
        var ws = workspaces.findById(workspaceId).orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        ws.setAttachmentMaxBytes(limit);
        workspaces.save(ws);
        return new Settings(limit,appMax);
    }
}
