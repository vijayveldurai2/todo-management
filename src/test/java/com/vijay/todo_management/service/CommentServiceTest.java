package com.vijay.todo_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.mapper.CommentMapper;
import com.vijay.todo_management.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentRepository comments;
    @Mock TodoRepository todos;
    @Mock ProjectRepository projects;
    @Mock ProjectMemberRepository members;
    @Mock WorkspaceMemberRepository workspaceMembers;
    @Mock UserRepository users;
    CommentService service;
    UUID uid = UUID.randomUUID(), tid = UUID.randomUUID(), cid = UUID.randomUUID();
    Project p; Todo t; User author; Comment c;
    CommentCreateRequest create;
    CommentUpdateRequest edit;
    tools.jackson.databind.JsonNode httpJson() {
        return tools.jackson.databind.json.JsonMapper.builder().build().readTree(c.getContentJson().toString());
    }
    @BeforeEach void setup() throws Exception {
        service = new CommentService(comments, todos, projects, members, workspaceMembers, users, new CommentMapper());
        Workspace ws = new Workspace(); ws.setId(UUID.randomUUID());
        p = new Project(); p.setId(UUID.randomUUID()); p.setWorkspace(ws);
        t = new Todo(); t.setId(tid); t.setProject(p);
        author = new User(); author.setId(uid); author.setName("Author");
        c = new Comment(); c.setId(cid); c.setTodo(t); c.setAuthor(author);
        c.setContentPlainText("Original");
        c.setContentJson(new ObjectMapper().readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Original\"}]}]}"));
        create = new CommentCreateRequest(); create.setContentJson(httpJson()); create.setContentPlainText("Original");
        edit = new CommentUpdateRequest(); edit.setContentJson(httpJson()); edit.setContentPlainText("Edited");
        when(projects.findByWorkspace_SlugAndSlug("ws", "p")).thenReturn(Optional.of(p));
    }
    void existing() {
        when(todos.findByIdAndProject_Id(tid, p.getId())).thenReturn(Optional.of(t));
        when(comments.findByIdAndTodo_Id(cid, tid)).thenReturn(Optional.of(c));
    }
    void creating() {
        when(members.existsByProject_IdAndUser_Id(p.getId(), uid)).thenReturn(true);
        when(todos.findForCommentWrite(tid, p.getId())).thenReturn(Optional.of(t));
    }
    @Test void formerAuthorCanEditWithoutMembership() {
        existing(); when(comments.saveAndFlush(c)).thenReturn(c);
        assertEquals("Edited", service.edit("ws","p",tid,cid,edit,uid).contentPlainText());
        verifyNoInteractions(members, workspaceMembers);
    }
    @Test void nonAuthorIncludingSuperAdminCannotEdit() {
        existing();
        assertThrows(ForbiddenException.class, () -> service.edit("ws","p",tid,cid,edit,UUID.randomUUID()));
        verifyNoInteractions(workspaceMembers);
    }
    @Test void deletedAuthorEditConflicts() {
        existing(); c.setDeleted(true);
        assertThrows(ResourceConflictException.class, () -> service.edit("ws","p",tid,cid,edit,uid));
    }
    @Test void deletePreservesContentAndFirstAudit() {
        existing(); when(users.getReferenceById(uid)).thenReturn(author);
        var json = c.getContentJson();
        service.delete("ws","p",tid,cid,uid);
        var at = c.getDeletedAt();
        service.delete("ws","p",tid,cid,uid);
        assertTrue(c.isDeleted()); assertSame(json,c.getContentJson());
        assertEquals("Original",c.getContentPlainText());
        assertEquals(at,c.getDeletedAt()); assertSame(author,c.getDeletedBy());
        verify(comments,times(1)).saveAndFlush(c);
    }
    @Test void unauthorizedRepeatedDeletionStillForbidden() {
        existing(); c.setDeleted(true);
        assertThrows(ForbiddenException.class, () -> service.delete("ws","p",tid,cid,UUID.randomUUID()));
    }
    @Test void projectAdminCanModerate() {
        existing(); UUID admin = UUID.randomUUID();
        ProjectRole role = new ProjectRole(); role.setAdmin(true);
        ProjectMember member = new ProjectMember(); member.setProjectRole(role);
        when(members.findByProject_IdAndUser_Id(p.getId(),admin)).thenReturn(Optional.of(member));
        service.delete("ws","p",tid,cid,admin);
        assertTrue(c.isDeleted());
    }
    @Test void superAdminCanModerate() {
        existing(); UUID admin = UUID.randomUUID();
        WorkspaceMember member = new WorkspaceMember(); member.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(workspaceMembers.findByWorkspace_IdAndUser_Id(p.getWorkspace().getId(),admin)).thenReturn(Optional.of(member));
        service.delete("ws","p",tid,cid,admin);
        assertTrue(c.isDeleted());
    }
    @Test void nonMemberCannotCreateEvenAsSuperAdmin() {
        assertThrows(ForbiddenException.class, () -> service.create("ws","p",tid,create,uid));
        verifyNoInteractions(workspaceMembers);
    }
    @Test void nonMemberCannotList() {
        assertThrows(ForbiddenException.class, () -> service.list("ws","p",tid,uid));
    }
    @Test void superAdminCanListWithoutProjectMembership() {
        WorkspaceMember member = new WorkspaceMember(); member.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(workspaceMembers.findByWorkspace_IdAndUser_Id(p.getWorkspace().getId(),uid)).thenReturn(Optional.of(member));
        when(todos.findByIdAndProject_Id(tid,p.getId())).thenReturn(Optional.of(t));
        assertTrue(service.list("ws","p",tid,uid).isEmpty());
    }
    @Test void todoFromDifferentProjectCannotBeEditedOrDeleted() {
        assertThrows(ResourceNotFoundException.class, () -> service.edit("ws","p",tid,cid,edit,uid));
        assertThrows(ResourceNotFoundException.class, () -> service.delete("ws","p",tid,cid,uid));
        verifyNoInteractions(comments);
    }
    @Test void listMasksDeletedParentsAndKeepsReplies() {
        when(members.existsByProject_IdAndUser_Id(p.getId(),uid)).thenReturn(true);
        when(todos.findByIdAndProject_Id(tid,p.getId())).thenReturn(Optional.of(t));
        c.setDeleted(true);
        Comment reply = new Comment(); reply.setId(UUID.randomUUID()); reply.setTodo(t); reply.setAuthor(author);
        reply.setParentComment(c); reply.setContentPlainText("Reply"); reply.setContentJson(c.getContentJson());
        when(comments.findByTodo_IdOrderByCreatedAtAscIdAsc(tid)).thenReturn(List.of(c,reply));
        var result = service.list("ws","p",tid,uid);
        assertNull(result.get(0).contentJson()); assertEquals("[comment deleted]",result.get(0).contentPlainText());
        assertEquals(cid,result.get(1).parentCommentId()); assertEquals("Reply",result.get(1).contentPlainText());
    }
    @Test void replyToDeletedParentAllowed() {
        creating(); c.setDeleted(true); create.setParentCommentId(cid);
        when(comments.findByIdAndTodo_Id(cid,tid)).thenReturn(Optional.of(c));
        when(users.getReferenceById(uid)).thenReturn(author);
        when(comments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(cid,service.create("ws","p",tid,create,uid).parentCommentId());
    }
    @Test void crossTodoParentRejected() {
        creating(); create.setParentCommentId(cid);
        assertThrows(BadRequestException.class, () -> service.create("ws","p",tid,create,uid));
    }
    @Test void crossTodoEditAndDeleteRejected() {
        when(todos.findByIdAndProject_Id(tid,p.getId())).thenReturn(Optional.of(t));
        assertThrows(ResourceNotFoundException.class, () -> service.edit("ws","p",tid,cid,edit,uid));
        assertThrows(ResourceNotFoundException.class, () -> service.delete("ws","p",tid,cid,uid));
        verify(comments,never()).findById(any());
    }
    @Test void missingBlankAndUnsupportedContentRejectedOnCreate() throws Exception {
        creating();
        for (String invalid : List.of("null","{}","[]","1","{\"type\":\"doc\",\"content\":[]}","{\"type\":\"doc\",\"content\":[{}]}")) {
            create.setContentJson(tools.jackson.databind.json.JsonMapper.builder().build().readTree(invalid));
            assertThrows(BadRequestException.class, () -> service.create("ws","p",tid,create,uid));
        }
        create.setContentJson(null);
        assertThrows(BadRequestException.class, () -> service.create("ws","p",tid,create,uid));
        create.setContentJson(httpJson());
        for (String invalid : Arrays.asList(null,""," \t\n")) {
            create.setContentPlainText(invalid);
            assertThrows(BadRequestException.class, () -> service.create("ws","p",tid,create,uid));
        }
    }
    @Test void editsRequireBothFields() {
        existing(); edit.setContentJson(null);
        assertThrows(BadRequestException.class, () -> service.edit("ws","p",tid,cid,edit,uid));
        edit.setContentJson(httpJson()); edit.setContentPlainText(" ");
        assertThrows(BadRequestException.class, () -> service.edit("ws","p",tid,cid,edit,uid));
    }
}
