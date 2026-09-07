package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.Priority;
import com.vijay.todo_management.enums.StatusCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceConflictException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.mapper.TodoMapper;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TodoServiceImpl implements TodoService {

    @Autowired private TodoRepository todoRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private StatusRepository statusRepository;
    @Autowired private SprintBoardRepository sprintBoardRepository;
    @Autowired private TagsRepository tagsRepository;
    @Autowired private TodoAssignmentRepository todoAssignmentRepository;
    @Autowired private TodoRoleRepository todoRoleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TodoMapper todoMapper;
    @Autowired private ChecklistItemRepository checklistItemRepository;

    // ── Authorization ────────────────────────────────────────────────────────

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

    // ── Validation ───────────────────────────────────────────────────────────

    private void validateDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("startDateTime must not be after endDateTime");
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private TodoDto mapToDto(Todo todo) {
        if (todo == null) return null;
        ChecklistStatsProjection stats = checklistItemRepository.getStatsByTodoId(todo.getId()).orElse(null);
        return todoMapper.mapToDto(todo, stats);
    }

    private List<TodoDto> mapToDtoList(List<Todo> todos) {
        if (todos == null || todos.isEmpty()) return Collections.emptyList();
        Set<UUID> todoIds = todos.stream().map(Todo::getId).collect(Collectors.toSet());
        Map<UUID, ChecklistStatsProjection> statsMap = checklistItemRepository.getStatsByTodoIds(todoIds).stream()
                .collect(Collectors.toMap(ChecklistStatsProjection::getTodoId, s -> s));
        return todoMapper.mapToDtoList(todos, statsMap);
    }

    private Tags findOrCreateTag(String name) {
        return tagsRepository.findByName(name)
                .orElseGet(() -> {
                    Tags newTag = new Tags();
                    newTag.setName(name);
                    return tagsRepository.save(newTag);
                });
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TodoDto createTodo(String workspaceSlug, String projectSlug, TodoCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Title is required");
        }

        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validateDescriptionConsistency(request.getDescriptionJson(), request.getDescriptionPlainText());

        // Atomically increment displayIdSeq
        int seq = project.getDisplayIdSeq() + 1;
        project.setDisplayIdSeq(seq);
        projectRepository.save(project);
        String prefix = project.getPrefixCode() != null && !project.getPrefixCode().isEmpty()
                ? project.getPrefixCode() : "TD";
        String displayId = prefix + "-" + seq;

        // Resolve Status
        Status status;
        if (request.getStatusId() != null) {
            status = statusRepository.findByIdAndProject_Id(request.getStatusId(), project.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Status not found or does not belong to this project: " + request.getStatusId()));
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
                    .orElseThrow(() -> new BadRequestException(
                            "Sprint board not found or does not belong to this project: " + request.getSprintId()));
        }

        Todo todo = new Todo();
        todo.setProject(project);
        todo.setDisplayId(displayId);
        todo.setTitle(request.getTitle().trim());
        todo.setDescriptionJson(request.getDescriptionJson() != null && !request.getDescriptionJson().isNull()
                ? request.getDescriptionJson() : null);
        todo.setDescriptionPlainText(request.getDescriptionPlainText());
        todo.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        todo.setStatus(status);
        todo.setSprint(sprint);
        todo.setStartDateTime(request.getStartDateTime());
        todo.setEndDateTime(request.getEndDateTime());
        todo.setEstimatedTime(request.getEstimatedTime());
        todo.setRemainingTime(request.getRemainingTime());
        todo.setStoryPoints(request.getStoryPoints());

        if (request.getTagNames() != null) {
            todo.setTags(request.getTagNames().stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet()));
        }

        return mapToDto(todoRepository.save(todo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDto> getTodos(String workspaceSlug, String projectSlug, UUID sprintId, Boolean backlogOnly, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);

        List<Todo> todos;
        // Conflict precedence: sprintId wins over backlogOnly if both are present
        if (sprintId != null) {
            todos = todoRepository.findByProject_IdAndSprint_Id(project.getId(), sprintId);
        } else if (Boolean.TRUE.equals(backlogOnly)) {
            todos = todoRepository.findByProject_IdAndSprintIsNull(project.getId());
        } else {
            todos = todoRepository.findByProject_Id(project.getId());
        }

        return mapToDtoList(todos);
    }

    @Override
    @Transactional(readOnly = true)
    public TodoDto getTodoById(String workspaceSlug, String projectSlug, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));
        return mapToDto(todo);
    }

    @Override
    @Transactional
    public TodoDto updateTodo(String workspaceSlug, String projectSlug, UUID todoId, TodoUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        // Determine effective start/end for validation (merge request values with current stored values)
        java.time.LocalDateTime effectiveStart = request.getStartDateTime() != null
                ? request.getStartDateTime() : todo.getStartDateTime();
        java.time.LocalDateTime effectiveEnd = request.getEndDateTime() != null
                ? request.getEndDateTime() : todo.getEndDateTime();
        validateDateRange(effectiveStart, effectiveEnd);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            todo.setTitle(request.getTitle().trim());
        }
        if (request.isDescriptionUpdateRequested()) {
            boolean jsonIsNull = request.getDescriptionJson() == null || request.getDescriptionJson().isNull();
            boolean plainTextIsNull = request.getDescriptionPlainText() == null;

            if (jsonIsNull && plainTextIsNull) {
                // Explicit clear
                todo.setDescriptionJson(null);
                todo.setDescriptionPlainText(null);
            } else if (!jsonIsNull && !plainTextIsNull) {
                // Explicit update
                todo.setDescriptionJson(request.getDescriptionJson());
                todo.setDescriptionPlainText(request.getDescriptionPlainText());
            } else {
                // Inconsistent update (one provided, one null/omitted)
                throw new BadRequestException("descriptionJson and descriptionPlainText must either both be provided or both be null");
            }
        }
        if (request.getPriority() != null) {
            todo.setPriority(request.getPriority());
        }
        if (request.getTagNames() != null) {
            todo.setTags(request.getTagNames().stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet()));
        }
        if (request.getStartDateTime() != null) {
            todo.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            todo.setEndDateTime(request.getEndDateTime());
        }
        if (request.getEstimatedTime() != null) {
            todo.setEstimatedTime(request.getEstimatedTime());
        }
        if (request.getRemainingTime() != null) {
            todo.setRemainingTime(request.getRemainingTime());
        }
        if (request.getStoryPoints() != null) {
            todo.setStoryPoints(request.getStoryPoints());
        }

        return mapToDto(todoRepository.save(todo));
    }

    @Override
    @Transactional
    public void deleteTodo(String workspaceSlug, String projectSlug, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));
        todoRepository.delete(todo);
    }

    @Override
    @Transactional
    public TodoDto updateTodoStatus(String workspaceSlug, String projectSlug, UUID todoId, TodoStatusUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        if (request.getStatusId() == null) {
            throw new BadRequestException("statusId is required");
        }

        Status newStatus = statusRepository.findByIdAndProject_Id(request.getStatusId(), project.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Status not found or does not belong to this project: " + request.getStatusId()));

        todo.setStatus(newStatus);
        return mapToDto(todoRepository.save(todo));
    }

    @Override
    @Transactional
    public TodoDto assignSprint(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        SprintBoard sprint = sprintBoardRepository.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(project.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint board not found or does not belong to this project: " + sprintId));

        todo.setSprint(sprint);
        return mapToDto(todoRepository.save(todo));
    }

    @Override
    @Transactional
    public TodoDto removeSprintAssignment(String workspaceSlug, String projectSlug, UUID sprintId, UUID todoId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        if (todo.getSprint() == null || !todo.getSprint().getId().equals(sprintId)) {
            throw new BadRequestException("Todo is not assigned to sprint: " + sprintId);
        }

        todo.setSprint(null);
        return mapToDto(todoRepository.save(todo));
    }

    // ── Assignment Management ────────────────────────────────────────────────

    @Override
    @Transactional
    public TodoAssignmentDto addAssignment(String workspaceSlug, String projectSlug, UUID todoId,
                                           TodoAssignmentRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Todo todo = todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        User assignee = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        TodoRole role = todoRoleRepository.findByIdAndProject_Id(request.getTodoRoleId(), project.getId())
                .orElseThrow(() -> new BadRequestException(
                        "TodoRole not found or does not belong to this project: " + request.getTodoRoleId()));

        // Determine isPrimary: force true on first assignment; honour request thereafter
        boolean isFirst = todoAssignmentRepository.countByTodo_Id(todo.getId()) == 0;
        boolean setPrimary = isFirst || Boolean.TRUE.equals(request.getIsPrimary());

        // If this assignment should be primary, unset any existing primary first
        if (setPrimary) {
            todoAssignmentRepository.findByTodo_IdAndIsPrimaryTrue(todo.getId())
                    .ifPresent(prev -> {
                        prev.setPrimary(false);
                        todoAssignmentRepository.save(prev);
                    });
        }

        TodoAssignment assignment = new TodoAssignment();
        assignment.setTodo(todo);
        assignment.setUser(assignee);
        assignment.setTodoRole(role);
        assignment.setPrimary(setPrimary);

        try {
            return todoMapper.mapAssignmentToDto(todoAssignmentRepository.save(assignment));
        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(
                    "User " + request.getUserId() + " already has role '"
                    + role.getName() + "' on this todo");
        }
    }

    @Override
    @Transactional
    public void removeAssignment(String workspaceSlug, String projectSlug, UUID todoId, UUID assignmentId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        // Verify todo belongs to project
        todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        TodoAssignment assignment = todoAssignmentRepository.findByIdAndTodo_Id(assignmentId, todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        todoAssignmentRepository.delete(assignment);
    }

    @Override
    @Transactional
    public TodoAssignmentDto setPrimaryAssignment(String workspaceSlug, String projectSlug, UUID todoId,
                                                   UUID assignmentId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        todoRepository.findByIdAndProject_Id(todoId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found with id: " + todoId));

        TodoAssignment target = todoAssignmentRepository.findByIdAndTodo_Id(assignmentId, todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        // Unset current primary (if different from target)
        todoAssignmentRepository.findByTodo_IdAndIsPrimaryTrue(todoId)
                .ifPresent(prev -> {
                    if (!prev.getId().equals(target.getId())) {
                        prev.setPrimary(false);
                        todoAssignmentRepository.save(prev);
                    }
                });

        target.setPrimary(true);
        return todoMapper.mapAssignmentToDto(todoAssignmentRepository.save(target));
    }

    private void validateDescriptionConsistency(JsonNode json, String plainText) {
        boolean hasJson = json != null && !json.isNull();
        boolean hasPlainText = plainText != null;
        if (hasJson != hasPlainText) {
            throw new BadRequestException("descriptionJson and descriptionPlainText must either both be provided or both be null");
        }
    }
}
