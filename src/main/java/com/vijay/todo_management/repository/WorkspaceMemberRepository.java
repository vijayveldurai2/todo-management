package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    List<WorkspaceMember> findByWorkspace_Id(UUID workspaceId);
    List<WorkspaceMember> findByUser_Id(UUID userId);
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);
    boolean existsByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);
}
