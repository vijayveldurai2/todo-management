package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.Priority;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TodoServiceImpl implements TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private SprintBoardRepository sprintBoardRepository;

    @Autowired
    private TagsRepository tagsRepository;

    private StatusDto mapStatusToDto(Status status) {
        if (status == null) return null;
        StatusDto dto = new StatusDto();
        dto.setId(status.getId());
        dto.setProjectId(status.getProject() != null ? status.getProject().getId() : null);
        dto.setName(status.getName());
        dto.setCategory(status.getCategory());
        dto.setPosition(status.getPosition());
        dto.setCreatedAt(status.getCreatedAt());
        dto.setUpdatedAt(status.getUpdatedAt());
        return dto;
    }

    private TodoDto mapToDto(Todo todo) {
        if (todo == null) return null;
        TodoDto dto = new TodoDto();
        dto.setId(todo.getId());
        dto.setProjectId(todo.getProject() != null ? todo.getProject().getId() : null);
        dto.setDisplayId(todo.getDisplayId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setPriority(todo.getPriority());

        if (todo.getStatus() != null) {
            dto.setStatusId(todo.getStatus().getId());
            dto.setStatus(mapStatusToDto(todo.getStatus()));
            dto.setIsDone(todo.getStatus().getCategory() == StatusCategory.DONE);
        } else {
            dto.setIsDone(false);
        }

        if (todo.getSprint() != null) {
            dto.setSprintId(todo.getSprint().getId());
        }

        dto.setPosition(todo.getPosition());

        if (todo.getTags() != null) {
            Set<String> tagNames = todo.getTags().stream()
                    .map(Tags::getName)
                    .collect(Collectors.toSet());
            dto.setTagNames(tagNames);
        } else {
            dto.setTagNames(new HashSet<>());
        }

        dto.setCreatedDate(todo.getCreatedDate());
        dto.setModifiedDate(todo.getModifiedDate());
        dto.setDueDate(todo.getDueDate());
        return dto;
    }

    private Tags findOrCreateTag(String name) {
        return tagsRepository.findByName(name)
                .orElseGet(() -> {
                    Tags newTag = new Tags();
                    newTag.setName(name);
                    return tagsRepository.save(newTag);
                });
    }

    private Project getProjectAndValidateAccess(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectSlug + " in workspace: " + workspaceSlug));

        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId);

        if (!isSuperAdmin && !isProjectMember) {
            throw new ForbiddenException("Access denied: caller is not a member of this project or workspace");
        }

        return project;
    }

    @Override
    @Transactional
    public TodoDto createTodo(String workspaceSlug, String projectSlug, TodoCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Title is required");
        }

        // Atomically increment displayIdSeq
        int seq = project.getDisplayIdSeq() + 1;
        project.setDisplayIdSeq(seq);
        projectRepository.save(project);

        String prefix = project.getPrefixCode() != null && !project.getPrefixCode().isEmpty() ? project.getPrefixCode() : "TD";
        String displayId = prefix + "-" + seq;

        // Resolve Status
        Status status;
        if (request.getStatusId() != null) {
            status = statusRepository.findByIdAndProject_Id(request.getStatusId(), project.getId())
                    .orElseThrow(() -> new BadRequestException("Status not found or does not belong to this project: " + request.getStatusId()));
        } else {
            List<Status> projectStatuses = statusRepository.findByProject_IdOrderByPositionAsc(project.getId());
            status = projectStatuses.stream()
                    .filter(s -> s.getCategory() == StatusCategory.NOT_STARTED)
                    .findFirst()
                    .orElseGet(() -> projectStatuses.isEmpty() ? null : projectStatuses.get(0));

            if (status == null) {
                throw new BadRequestException("Project has no configured statuses");
            }
        }

        // Resolve Sprint (optional)
        SprintBoard sprint = null;
        if (request.getSprintId() != null) {
            sprint = sprintBoardRepository.findById(request.getSprintId())
                    .filter(s -> s.getProject().getId().equals(project.getId()))
                    .orElseThrow(() -> new BadRequestException("Sprint board not found or does not belong to this project: " + request.getSprintId()));
        }

        Todo todo = new Todo();
        todo.setProject(project);
        todo.setDisplayId(displayId);
        todo.setTitle(request.getTitle().trim());
        todo.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        todo.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        todo.setStatus(status);
        todo.setSprint(sprint);
        todo.setDueDate(request.getDueDate());

        if (request.getTagNames() != null) {
            Set<Tags> tags = request.getTagNames().stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet());
            todo.setTags(tags);
        }

        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDto> getTodos(String workspaceSlug, String projectSlug, UUID sprintId, Boolean backlogOnly, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        List<Todo> todos;
        // Conflict precedence rule: if sprintId != null, sprintId takes precedence and backlogOnly is ignored
        if (sprintId != null) {
            todos = todoRepository.findByProject_IdAndSprint_Id(project.getId(), sprintId);
        } else if (Boolean.TRUE.equals(backlogOnly)) {
            todos = todoRepository.findByProject_IdAndSprintIsNull(project.getId());
        } else {
            todos = todoRepository.findByProject_Id(project.getId());
        }

        return todos.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TodoDto getTodoById(String workspaceSlug, String projectSlug, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        return mapToDto(todo);
    }

    @Override
    @Transactional
    public TodoDto updateTodo(String workspaceSlug, String projectSlug, UUID todoId, TodoUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            todo.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription().trim());
        }
        if (request.getPriority() != null) {
            todo.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
        }
        if (request.getTagNames() != null) {
            Set<Tags> tags = request.getTagNames().stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet());
            todo.setTags(tags);
        }

        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteTodo(String workspaceSlug, String projectSlug, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        todoRepository.delete(todo);
    }

    @Override
    @Transactional
    public TodoDto updateTodoStatus(String workspaceSlug, String projectSlug, UUID todoId, TodoStatusUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        if (request.getStatusId() == null) {
            throw new BadRequestException("statusId is required");
        }

        Status newStatus = statusRepository.findByIdAndProject_Id(request.getStatusId(), project.getId())
                .orElseThrow(() -> new BadRequestException("Status not found or does not belong to this project: " + request.getStatusId()));

        todo.setStatus(newStatus);
        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TodoDto assignSprint(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        SprintBoard sprint = sprintBoardRepository.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(project.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Sprint board not found or does not belong to this project: " + sprintId));

        todo.setSprint(sprint);
        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TodoDto removeSprintAssignment(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId + " in this project"));

        if (todo.getSprint() == null || !todo.getSprint().getId().equals(sprintId)) {
            throw new BadRequestException("Todo is not assigned to sprint: " + sprintId);
        }

        todo.setSprint(null);
        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }
}
