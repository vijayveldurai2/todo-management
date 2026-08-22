package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.WorkspaceInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {
    Optional<WorkspaceInvite> findByTokenHash(String tokenHash);
    List<WorkspaceInvite> findByWorkspace_IdAndStatus(UUID workspaceId, WorkspaceInvite.Status status);
    Optional<WorkspaceInvite> findByWorkspace_IdAndEmailAndStatus(UUID workspaceId, String email, WorkspaceInvite.Status status);
    List<WorkspaceInvite> findByEmailAndStatus(String email, WorkspaceInvite.Status status);
}
