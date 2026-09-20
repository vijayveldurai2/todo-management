package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.ChecklistItemDto;
import com.vijay.todo_management.dto.ChecklistItemUpdateRequest;
import com.vijay.todo_management.entity.ChecklistItem;
import com.vijay.todo_management.entity.Project;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.ChecklistItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChecklistItemServiceImpl implements ChecklistItemService {

    @Autowired
    private ChecklistItemRepository checklistItemRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private Project getProjectAndValidateAccess(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectSlug + " in workspace: " + workspaceSlug));

        boolean isSuperAdmin = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId);

        if (!isSuperAdmin && !isProjectMember) {
            throw new ForbiddenException("Access denied: caller is not a member of this project or workspace");
        }
        return project;
    }

    private Todo getTodoAndValidate(Project project, UUID todoId) {
        return todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));
    }

    private ChecklistItemDto mapToDto(ChecklistItem item) {
        if (item == null) return null;
        return new ChecklistItemDto(
                item.getId(),
                item.getTodo() != null ? item.getTodo().getId() : null,
                item.getText(),
                item.isChecked(),
                item.getPosition(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistItemDto> getChecklistItems(String workspaceSlug, String projectSlug, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = getTodoAndValidate(project, todoId);
        return checklistItemRepository.findByTodo_IdOrderByPositionAsc(todo.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChecklistItemDto addItem(String workspaceSlug, String projectSlug, UUID todoId,
                                    ChecklistItemCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = getTodoAndValidate(project, todoId);

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new BadRequestException("Text is required");
        }

        int currentCount = checklistItemRepository.countByTodo_Id(todo.getId());
        int targetPosition;

        if (request.getPosition() == null || request.getPosition() >= currentCount) {
            targetPosition = currentCount;
        } else {
            targetPosition = Math.max(0, request.getPosition());
            List<ChecklistItem> subsequent = checklistItemRepository
                    .findByTodoIdAndPositionGreaterThanEqual(todo.getId(), targetPosition);
            for (ChecklistItem item : subsequent) {
                item.setPosition(item.getPosition() + 1);
            }
            checklistItemRepository.saveAll(subsequent);
        }

        ChecklistItem newItem = new ChecklistItem();
        newItem.setTodo(todo);
        newItem.setText(request.getText().trim());
        newItem.setChecked(false);
        newItem.setPosition(targetPosition);

        return mapToDto(checklistItemRepository.save(newItem));
    }

    @Override
    @Transactional
    public ChecklistItemDto updateItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId,
                                       ChecklistItemUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = getTodoAndValidate(project, todoId);

        ChecklistItem item = checklistItemRepository.findByIdAndTodo_Id(itemId, todo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (request.getText() != null) {
            if (request.getText().trim().isEmpty()) {
                throw new BadRequestException("Text cannot be empty");
            }
            item.setText(request.getText().trim());
        }

        if (request.getIsChecked() != null) {
            item.setChecked(request.getIsChecked());
        }

        return mapToDto(checklistItemRepository.save(item));
    }

    @Override
    @Transactional
    public ChecklistItemDto reorderItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId,
                                        Integer newPosition, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = getTodoAndValidate(project, todoId);

        ChecklistItem item = checklistItemRepository.findByIdAndTodo_Id(itemId, todo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (newPosition == null) {
            throw new BadRequestException("Position is required for reordering");
        }

        int oldPos = item.getPosition();
        int currentCount = checklistItemRepository.countByTodo_Id(todo.getId());

        int targetPos = newPosition;
        if (targetPos < 0) targetPos = 0;
        if (targetPos >= currentCount) targetPos = currentCount - 1;

        if (oldPos != targetPos) {
            if (oldPos < targetPos) {
                List<ChecklistItem> between = checklistItemRepository
                        .findByTodoIdAndPositionBetween(todo.getId(), oldPos + 1, targetPos);
                for (ChecklistItem c : between) {
                    c.setPosition(c.getPosition() - 1);
                }
                checklistItemRepository.saveAll(between);
            } else {
                List<ChecklistItem> between = checklistItemRepository
                        .findByTodoIdAndPositionBetween(todo.getId(), targetPos, oldPos - 1);
                for (ChecklistItem c : between) {
                    c.setPosition(c.getPosition() + 1);
                }
                checklistItemRepository.saveAll(between);
            }
            item.setPosition(targetPos);
            checklistItemRepository.save(item);
        }

        return mapToDto(item);
    }

    @Override
    @Transactional
    public void deleteItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = getTodoAndValidate(project, todoId);

        ChecklistItem item = checklistItemRepository.findByIdAndTodo_Id(itemId, todo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        int deletedPos = item.getPosition();
        checklistItemRepository.delete(item);

        List<ChecklistItem> subsequent = checklistItemRepository
                .findByTodoIdAndPositionGreaterThanEqual(todo.getId(), deletedPos + 1);
        for (ChecklistItem c : subsequent) {
            c.setPosition(c.getPosition() - 1);
        }
        checklistItemRepository.saveAll(subsequent);
    }
}
