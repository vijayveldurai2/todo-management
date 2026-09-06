package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.ChecklistItemDto;
import com.vijay.todo_management.dto.ChecklistItemUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ChecklistItemService {

    List<ChecklistItemDto> getChecklistItems(String workspaceSlug, String projectSlug, UUID todoId, UUID userId);

    ChecklistItemDto addItem(String workspaceSlug, String projectSlug, UUID todoId, ChecklistItemCreateRequest request, UUID userId);

    ChecklistItemDto updateItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId, ChecklistItemUpdateRequest request, UUID userId);

    ChecklistItemDto reorderItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId, Integer newPosition, UUID userId);

    void deleteItem(String workspaceSlug, String projectSlug, UUID todoId, UUID itemId, UUID userId);
}
