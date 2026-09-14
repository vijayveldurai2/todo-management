package com.vijay.todo_management.mapper;

import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.dto.TodoAssignmentDto;
import com.vijay.todo_management.dto.TodoDto;
import com.vijay.todo_management.entity.Status;
import com.vijay.todo_management.entity.Tags;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.entity.TodoAssignment;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.repository.ChecklistStatsProjection;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TodoMapper {

    public StatusDto mapStatusToDto(Status status) {
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

    public TodoAssignmentDto mapAssignmentToDto(TodoAssignment a) {
        if (a == null) return null;
        TodoAssignmentDto dto = new TodoAssignmentDto();
        dto.setId(a.getId());
        dto.setTodoId(a.getTodo() != null ? a.getTodo().getId() : null);
        if (a.getUser() != null) {
            dto.setUserId(a.getUser().getId());
            String displayName = a.getUser().getName() != null && !a.getUser().getName().isBlank()
                    ? a.getUser().getName() : a.getUser().getUsername();
            dto.setUserDisplayName(displayName);
            dto.setUserEmail(a.getUser().getEmail());
            dto.setUserAvatarUrl(a.getUser().getAvatarUrl());
        }
        if (a.getTodoRole() != null) {
            dto.setTodoRoleId(a.getTodoRole().getId());
            dto.setTodoRoleName(a.getTodoRole().getName());
        }
        dto.setPrimary(a.isPrimary());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    public void applyChecklistStats(TodoDto dto, long total, long completed) {
        dto.setChecklistTotalCount((int) total);
        dto.setChecklistCompletedCount((int) completed);
        if (total == 0) {
            dto.setChecklistProgressPercentage(null);
        } else {
            dto.setChecklistProgressPercentage(Math.round(((float) completed / total) * 100));
        }
    }

    public TodoDto mapToDto(Todo todo, long totalChecklistCount, long completedChecklistCount) {
        if (todo == null) return null;
        TodoDto dto = new TodoDto();
        dto.setId(todo.getId());
        dto.setProjectId(todo.getProject() != null ? todo.getProject().getId() : null);
        dto.setParentTodoId(todo.getParentTodo() != null ? todo.getParentTodo().getId() : null);
        dto.setDisplayId(todo.getDisplayId());
        dto.setTitle(todo.getTitle());
        dto.setDescriptionJson(todo.getDescriptionJson());
        dto.setDescriptionPlainText(todo.getDescriptionPlainText());
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
            dto.setTagNames(todo.getTags().stream().map(Tags::getName).collect(Collectors.toSet()));
        } else {
            dto.setTagNames(new HashSet<>());
        }

        // Scheduling & effort
        dto.setStartDateTime(todo.getStartDateTime());
        dto.setEndDateTime(todo.getEndDateTime());
        dto.setEstimatedTime(todo.getEstimatedTime());
        dto.setRemainingTime(todo.getRemainingTime());
        dto.setStoryPoints(todo.getStoryPoints());

        // Assignments (loaded via OneToMany)
        if (todo.getAssignments() != null) {
            dto.setAssignments(todo.getAssignments().stream()
                    .map(this::mapAssignmentToDto)
                    .collect(Collectors.toList()));
        }

        applyChecklistStats(dto, totalChecklistCount, completedChecklistCount);

        dto.setCreatedDate(todo.getCreatedDate());
        dto.setModifiedDate(todo.getModifiedDate());
        return dto;
    }

    public TodoDto mapToDto(Todo todo, ChecklistStatsProjection stats) {
        long total = stats != null ? stats.getTotalCount() : 0;
        long completed = stats != null ? stats.getCompletedCount() : 0;
        return mapToDto(todo, total, completed);
    }

    public TodoDto mapToDto(Todo todo) {
        return mapToDto(todo, 0L, 0L);
    }

    public List<TodoDto> mapToDtoList(List<Todo> todos, Map<UUID, ChecklistStatsProjection> statsMap) {
        if (todos == null) return Collections.emptyList();
        return todos.stream()
                .map(todo -> mapToDto(todo, statsMap != null ? statsMap.get(todo.getId()) : null))
                .collect(Collectors.toList());
    }
}
