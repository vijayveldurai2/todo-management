package com.vijay.todo_management.controller;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.AttachmentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceId}/attachment-settings")
public class AttachmentSettingsController {
    private final AttachmentSettingsService service;
    public record Update(Long maxFileBytes) {}
    @GetMapping
    public AttachmentSettingsService.Settings get(@PathVariable UUID workspaceId) {
        return service.get(workspaceId,CurrentUserProvider.getCurrentUserId());
    }
    @PutMapping
    public AttachmentSettingsService.Settings update(@PathVariable UUID workspaceId, @RequestBody Update request) {
        return service.update(workspaceId,CurrentUserProvider.getCurrentUserId(),request.maxFileBytes());
    }
}
