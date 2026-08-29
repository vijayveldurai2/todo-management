package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ProjectMemberDto;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberService {
    List<ProjectMemberDto> getMembers(String projectSlug, UUID currentUserId);
    ProjectMemberDto addMember(String projectSlug, UUID userId, UUID roleId, UUID currentUserId);
    ProjectMemberDto changeMemberRole(String projectSlug, UUID userId, UUID roleId, UUID currentUserId);
    void removeMember(String projectSlug, UUID userId, UUID currentUserId);
}
