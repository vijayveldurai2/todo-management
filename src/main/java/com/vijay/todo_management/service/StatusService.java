package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.StatusCreateRequest;
import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.dto.StatusUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface StatusService {

    List<StatusDto> getStatuses(String workspaceSlug, String projectSlug, UUID userId);

    StatusDto createStatus(String workspaceSlug, String projectSlug, StatusCreateRequest request, UUID userId);

    StatusDto updateStatus(String workspaceSlug, String projectSlug, UUID statusId, StatusUpdateRequest request, UUID userId);

    void deleteStatus(String workspaceSlug, String projectSlug, UUID statusId, UUID userId);
}
