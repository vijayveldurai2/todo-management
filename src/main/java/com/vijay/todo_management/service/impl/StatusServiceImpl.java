package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.StatusCreateRequest;
import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.dto.StatusUpdateRequest;
import com.vijay.todo_management.entity.Project;
import com.vijay.todo_management.entity.Status;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.BoardColumnRepository;
import com.vijay.todo_management.repository.ProjectMemberRepository;
import com.vijay.todo_management.repository.ProjectRepository;
import com.vijay.todo_management.repository.StatusRepository;
import com.vijay.todo_management.repository.WorkspaceMemberRepository;
import com.vijay.todo_management.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatusServiceImpl implements StatusService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    private StatusDto mapToDto(Status status) {
        StatusDto dto = new StatusDto();
        dto.setId(status.getId());
        dto.setProjectId(status.getProject().getId());
        dto.setName(status.getName());
        dto.setCategory(status.getCategory());
        dto.setPosition(status.getPosition());
        dto.setCreatedAt(status.getCreatedAt());
        dto.setUpdatedAt(status.getUpdatedAt());
        return dto;
    }

    private Project getProjectAndValidateAccess(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with slug: " + projectSlug + " in workspace: " + workspaceSlug));

        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId);

        if (!isSuperAdmin && !isProjectMember) {
            throw new ForbiddenException("Access denied: caller is not a member of this project or workspace");
        }

        return project;
    }

    private void validateProjectAdmin(Project project, UUID userId) {
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        if (isSuperAdmin) {
            return;
        }

        boolean isProjectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .map(pm -> pm.getProjectRole().isAdmin())
                .orElse(false);

        if (!isProjectAdmin) {
            throw new ForbiddenException("Caller must be a project Admin or workspace SUPER_ADMIN to modify project statuses");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusDto> getStatuses(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        return statusRepository.findByProject_IdOrderByPositionAsc(project.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StatusDto createStatus(String workspaceSlug, String projectSlug, StatusCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        if (statusRepository.existsByProject_IdAndNameIgnoreCase(project.getId(), request.getName().trim())) {
            throw new ResourceConflictException("Status with name '" + request.getName().trim() + "' already exists in this project");
        }

        int currentCount = (int) statusRepository.countByProject_Id(project.getId());
        int targetPosition = currentCount;

        if (request.getPosition() != null && request.getPosition() >= 0 && request.getPosition() < currentCount) {
            targetPosition = request.getPosition();
            // Shift subsequent statuses +1
            List<Status> toShift = statusRepository.findByProjectIdAndPositionGreaterThanEqual(project.getId(), targetPosition);
            for (Status s : toShift) {
                s.setPosition(s.getPosition() + 1);
            }
            statusRepository.saveAll(toShift);
        }

        Status status = new Status();
        status.setProject(project);
        status.setName(request.getName().trim());
        status.setCategory(request.getCategory());
        status.setPosition(targetPosition);

        Status saved = statusRepository.save(status);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public StatusDto updateStatus(String workspaceSlug, String projectSlug, UUID statusId, StatusUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Status status = statusRepository.findByIdAndProject_Id(statusId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + statusId + " in this project"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(status.getName()) && statusRepository.existsByProject_IdAndNameIgnoreCaseAndIdNot(project.getId(), newName, statusId)) {
                throw new ResourceConflictException("Status with name '" + newName + "' already exists in this project");
            }
            status.setName(newName);
        }

        if (request.getCategory() != null) {
            status.setCategory(request.getCategory());
        }

        if (request.getPosition() != null) {
            int oldPos = status.getPosition();
            int newPos = request.getPosition();
            int currentCount = (int) statusRepository.countByProject_Id(project.getId());

            if (newPos < 0) newPos = 0;
            if (newPos >= currentCount) newPos = currentCount - 1;

            if (oldPos != newPos) {
                if (oldPos < newPos) {
                    List<Status> between = statusRepository.findByProjectIdAndPositionBetween(project.getId(), oldPos + 1, newPos);
                    for (Status s : between) {
                        s.setPosition(s.getPosition() - 1);
                    }
                    statusRepository.saveAll(between);
                } else {
                    List<Status> between = statusRepository.findByProjectIdAndPositionBetween(project.getId(), newPos, oldPos - 1);
                    for (Status s : between) {
                        s.setPosition(s.getPosition() + 1);
                    }
                    statusRepository.saveAll(between);
                }
                status.setPosition(newPos);
            }
        }

        return mapToDto(statusRepository.save(status));
    }

    @Override
    @Transactional
    public void deleteStatus(String workspaceSlug, String projectSlug, UUID statusId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Status status = statusRepository.findByIdAndProject_Id(statusId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + statusId + " in this project"));

        // Status deletion guards:
        // TODO: status deletion guard once Todo.status_id is wired — check if any Todo references this status
        if (boardColumnRepository.existsByPrimaryStatus_Id(statusId)) {
            throw new ResourceConflictException("Cannot delete status: it is configured as the primary status for one or more board columns. Reassign or delete those columns first.");
        }

        if (boardColumnRepository.existsByAdditionalStatuses_Id(statusId)) {
            throw new ResourceConflictException("Cannot delete status: it is mapped in additional grouped statuses for one or more board columns. Remove the status from those columns first.");
        }

        int deletedPosition = status.getPosition();
        statusRepository.delete(status);

        // Close the gap by shifting subsequent positions -1
        List<Status> subsequent = statusRepository.findByProjectIdAndPositionGreaterThanEqual(project.getId(), deletedPosition + 1);
        for (Status s : subsequent) {
            s.setPosition(s.getPosition() - 1);
        }
        statusRepository.saveAll(subsequent);
    }
}
