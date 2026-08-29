package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProject_Slug(String projectSlug);
    Optional<ProjectMember> findByProject_SlugAndUser_Id(String projectSlug, UUID userId);
    Optional<ProjectMember> findByProject_IdAndUser_Id(UUID projectId, UUID userId);
    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);
    boolean existsByProject_SlugAndUser_Id(String projectSlug, UUID userId);
    long countByProjectRole_Id(UUID roleId);
}
