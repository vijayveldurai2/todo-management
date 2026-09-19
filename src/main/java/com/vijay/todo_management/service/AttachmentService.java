package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.AttachmentDto;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.storage.AttachmentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class AttachmentService {
    private final AttachmentRepository attachments;
    private final ProjectRepository projects;
    private final TodoRepository todos;
    private final ProjectMemberRepository members;
    private final WorkspaceMemberRepository workspaceMembers;
    private final UserRepository users;
    private final AttachmentStorage storage;
    @Value("${app.attachments.default-max-bytes:10485760}") private long defaultMax = 10485760;
    @Value("${app.attachments.max-bytes:26214400}") private long appMax = 26214400;

    private boolean superAdmin(Project p, UUID user) {
        return workspaceMembers.findByWorkspace_IdAndUser_Id(p.getWorkspace().getId(), user)
                .map(m -> m.getRole() == WorkspaceMember.Role.SUPER_ADMIN).orElse(false);
    }
    private Project access(String ws, String slug, UUID user) {
        Project p = projects.findByWorkspace_SlugAndSlug(ws, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!members.existsByProject_IdAndUser_Id(p.getId(), user) && !superAdmin(p, user))
            throw new ForbiddenException("Project membership or workspace SUPER_ADMIN required");
        return p;
    }
    private Todo todo(Project p, UUID id, boolean lock) {
        return (lock ? todos.findForCommentWrite(id, p.getId()) : todos.findByIdAndProject_Id(id, p.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found"));
    }
    private TodoAttachment attachment(UUID id, UUID todoId) {
        return attachments.findByIdAndTodo_IdAndDeletedFalse(id, todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found in this Todo"));
    }
    private AttachmentDto dto(TodoAttachment a) {
        User u = a.getUploadedBy();
        return new AttachmentDto(a.getId(), a.getTodo().getId(), a.getFileName(), a.getSizeBytes(),
                u.getId(), u.getName() == null || u.getName().isBlank() ? u.getUsername() : u.getName(),
                a.getCreatedAt());
    }
    private String filename(String name) {
        if (name == null) throw new BadRequestException("Filename is required");
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.isBlank() || base.equals(".") || base.equals("..") || base.length() > 255)
            throw new BadRequestException("Filename must contain 1 to 255 characters");
        return base;
    }
    @Transactional(readOnly = true)
    public List<AttachmentDto> list(String ws, String slug, UUID todoId, UUID user) {
        Project p = access(ws, slug, user);
        todo(p, todoId, false);
        return attachments.findByTodo_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(todoId).stream().map(this::dto).toList();
    }
    @Transactional(timeout = 120, rollbackFor = IOException.class)
    public AttachmentDto upload(String ws, String slug, UUID todoId, MultipartFile file, UUID user) throws IOException {
        Project p = access(ws, slug, user);
        // Lock serializes upload with Todo deletion. SUPER_ADMIN must still join to upload.
        if (!members.existsByProject_IdAndUser_Id(p.getId(), user))
            throw new ForbiddenException("Project membership required to upload");
        Todo t = todo(p, todoId, true);
        long limit = Math.min(appMax, p.getWorkspace().getAttachmentMaxBytes() == null
                ? defaultMax : p.getWorkspace().getAttachmentMaxBytes());
        if (file == null || file.isEmpty()) throw new BadRequestException("A non-empty file is required");
        if (file.getSize() > limit) throw new AttachmentTooLargeException("File exceeds the workspace upload limit");
        String displayName = filename(file.getOriginalFilename());
        String key = UUID.randomUUID().toString();
        // Rollback can happen at commit, after saveAndFlush has returned.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { storage.delete(key); }
                    catch (IOException ex) { log.warn("Rolled-back upload queued for orphan cleanup: {}", key); }
                }
            }
        });
        long size;
        try (InputStream in = file.getInputStream()) { size = storage.write(key, in, limit); }
        if (size == 0) throw new BadRequestException("A non-empty file is required");
        TodoAttachment a = new TodoAttachment();
        a.setTodo(t); a.setUploadedBy(users.getReferenceById(user));
        a.setStorageKey(key); a.setFileName(displayName); a.setSizeBytes(size);
        return dto(attachments.saveAndFlush(a));
    }
    public record Download(AttachmentDto metadata, InputStream stream) {}
    @Transactional(readOnly = true)
    public Download download(String ws, String slug, UUID todoId, UUID id, UUID user) throws IOException {
        Project p = access(ws, slug, user);
        todo(p, todoId, false);
        TodoAttachment a = attachment(id, todoId);
        AttachmentDto metadata = dto(a);
        try { return new Download(metadata, storage.open(a.getStorageKey())); }
        catch (NoSuchFileException ex) { throw new ResourceNotFoundException("Attachment file is unavailable"); }
    }
    @Transactional
    public void delete(String ws, String slug, UUID todoId, UUID id, UUID user) {
        Project p = access(ws, slug, user);
        todo(p, todoId, true);
        TodoAttachment a = attachment(id, todoId);
        boolean admin = members.findByProject_IdAndUser_Id(p.getId(), user)
                .map(m -> m.getProjectRole() != null && m.getProjectRole().isAdmin()).orElse(false);
        if (!a.getUploadedBy().getId().equals(user) && !admin && !superAdmin(p, user))
            throw new ForbiddenException("Only the uploader or an admin may delete an attachment");
        a.setDeleted(true);
        attachments.saveAndFlush(a); // Persistent cleanup marker, hidden from downloads immediately.
    }
}
