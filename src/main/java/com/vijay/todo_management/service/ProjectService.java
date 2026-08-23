package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ProjectDto;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ProjectDto createProject(String workspaceSlug, ProjectDto dto, UUID creatorId);
    List<ProjectDto> getProjectsByWorkspace(String workspaceSlug, UUID userId);
    ProjectDto getProjectBySlug(String workspaceSlug, String projectSlug, UUID userId);
    ProjectDto updateProject(String workspaceSlug, String projectSlug, ProjectDto dto, UUID userId);
    void archiveProject(String workspaceSlug, String projectSlug, UUID userId);
}
