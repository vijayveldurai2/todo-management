package com.vijay.todo_management.service;

import tools.jackson.databind.JsonNode;
import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.mapper.CommentMapper;
import com.vijay.todo_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final com.fasterxml.jackson.databind.ObjectMapper STORAGE_JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private final CommentRepository comments;
    private final TodoRepository todos;
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final WorkspaceMemberRepository workspaceMembers;
    private final UserRepository users;
    private final CommentMapper mapper;

    private Project project(String ws, String slug) {
        return projects.findByWorkspace_SlugAndSlug(ws, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
    private Todo todo(Project p, UUID id, boolean lock) {
        return (lock ? todos.findForCommentWrite(id, p.getId()) : todos.findByIdAndProject_Id(id, p.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found"));
    }
    private Comment comment(UUID id, UUID todoId) {
        return comments.findByIdAndTodo_Id(id, todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found in this Todo"));
    }
    private boolean superAdmin(Project p, UUID user) {
        return workspaceMembers.findByWorkspace_IdAndUser_Id(p.getWorkspace().getId(), user)
                .map(m -> m.getRole() == WorkspaceMember.Role.SUPER_ADMIN).orElse(false);
    }
    private boolean admin(Project p, UUID user) {
        return members.findByProject_IdAndUser_Id(p.getId(), user)
                .map(m -> m.getProjectRole() != null && m.getProjectRole().isAdmin()).orElse(false);
    }
    private void validate(JsonNode json, String text) {
        // Trusted client contract: plaintext must correspond to JSON, but is not derived here.
        // Minimal envelope: {"type":"doc","content":[{"type":"paragraph", ...}]}.
        // Nested editor schema is intentionally not interpreted.
        if (text == null || text.isBlank() || json == null || !json.isObject()
                || !"doc".equals(json.path("type").asText())
                || !json.path("content").isArray() || json.path("content").isEmpty()) {
            throw new BadRequestException("Non-blank contentPlainText and a non-empty doc contentJson are required");
        }
        for (JsonNode node : json.path("content")) {
            if (!node.isObject() || !node.path("type").isTextual() || node.path("type").asText().isBlank()) {
                throw new BadRequestException("Each content node must be an object with a non-blank type");
            }
        }
    }

    // Spring MVC uses Jackson 3; Hibernate's existing JSON mapping uses Jackson 2.
    // Bridge only at this boundary so neither library treats the other's tree as a POJO.
    private com.fasterxml.jackson.databind.JsonNode storageJson(JsonNode json) {
        try {
            return STORAGE_JSON.readTree(json.toString());
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new BadRequestException("Invalid comment JSON");
        }
    }

    @Transactional(readOnly = true)
    public List<CommentDto> list(String ws, String slug, UUID todoId, UUID user) {
        Project p = project(ws, slug);
        if (!members.existsByProject_IdAndUser_Id(p.getId(), user) && !superAdmin(p, user))
            throw new ForbiddenException("Project membership or workspace SUPER_ADMIN required");
        todo(p, todoId, false);
        // Pagination intentionally deferred. Author graph avoids one query per comment.
        return comments.findByTodo_IdOrderByCreatedAtAscIdAsc(todoId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CommentDto create(String ws, String slug, UUID todoId, CommentCreateRequest request, UUID user) {
        Project p = project(ws, slug);
        // SUPER_ADMIN does not bypass membership for creation.
        if (!members.existsByProject_IdAndUser_Id(p.getId(), user))
            throw new ForbiddenException("Project membership required to comment");
        Todo t = todo(p, todoId, true);
        validate(request.getContentJson(), request.getContentPlainText());
        Comment parent = request.getParentCommentId() == null ? null :
                comments.findByIdAndTodo_Id(request.getParentCommentId(), todoId)
                        .orElseThrow(() -> new BadRequestException("Parent comment must belong to this Todo"));
        Comment c = new Comment();
        c.setTodo(t);
        c.setAuthor(users.getReferenceById(user));
        c.setParentComment(parent); // Deleted parents are valid.
        c.setContentJson(storageJson(request.getContentJson()));
        c.setContentPlainText(request.getContentPlainText());
        return mapper.toDto(comments.saveAndFlush(c));
    }

    @Transactional
    public CommentDto edit(String ws, String slug, UUID todoId, UUID id, CommentUpdateRequest request, UUID user) {
        Project p = project(ws, slug);
        todo(p, todoId, false);
        Comment c = comment(id, todoId);
        // Authorship only, including former members. No SUPER_ADMIN override.
        if (!c.getAuthor().getId().equals(user)) throw new ForbiddenException("Only the author may edit");
        if (c.isDeleted()) throw new ResourceConflictException("Deleted comments cannot be edited");
        validate(request.getContentJson(), request.getContentPlainText());
        c.setContentJson(storageJson(request.getContentJson()));
        c.setContentPlainText(request.getContentPlainText());
        return mapper.toDto(comments.saveAndFlush(c));
    }

    @Transactional
    public void delete(String ws, String slug, UUID todoId, UUID id, UUID user) {
        Project p = project(ws, slug);
        todo(p, todoId, false);
        Comment c = comment(id, todoId);
        // Former authors retain deletion rights; moderators must have current privileges.
        if (!c.getAuthor().getId().equals(user) && !admin(p, user) && !superAdmin(p, user))
            throw new ForbiddenException("Only the author or an admin may delete");
        if (c.isDeleted()) return; // Preserve the first deletion's audit and version.
        c.setDeleted(true);
        c.setDeletedAt(LocalDateTime.now());
        c.setDeletedBy(users.getReferenceById(user));
        comments.saveAndFlush(c); // Original content and replies remain untouched.
    }
}
