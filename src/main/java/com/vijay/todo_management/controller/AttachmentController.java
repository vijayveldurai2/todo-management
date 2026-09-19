package com.vijay.todo_management.controller;
import com.vijay.todo_management.dto.AttachmentDto;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/attachments")
public class AttachmentController {
    private final AttachmentService service;
    @GetMapping
    public List<AttachmentDto> list(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId) {
        return service.list(workspaceSlug, projectSlug, todoId, CurrentUserProvider.getCurrentUserId());
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDto> upload(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId, @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(201).body(service.upload(workspaceSlug, projectSlug, todoId, file,
                CurrentUserProvider.getCurrentUserId()));
    }
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String workspaceSlug,
            @PathVariable String projectSlug, @PathVariable UUID todoId, @PathVariable UUID attachmentId) throws IOException {
        var result = service.download(workspaceSlug, projectSlug, todoId, attachmentId, CurrentUserProvider.getCurrentUserId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(result.metadata().sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.metadata().fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(new InputStreamResource(result.stream()));
    }
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable String workspaceSlug, @PathVariable String projectSlug,
            @PathVariable UUID todoId, @PathVariable UUID attachmentId) {
        service.delete(workspaceSlug, projectSlug, todoId, attachmentId, CurrentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
