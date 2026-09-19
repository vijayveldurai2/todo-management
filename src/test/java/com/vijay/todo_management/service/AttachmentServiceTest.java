package com.vijay.todo_management.service;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.storage.AttachmentStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock AttachmentRepository attachments;
    @Mock ProjectRepository projects;
    @Mock TodoRepository todos;
    @Mock ProjectMemberRepository members;
    @Mock WorkspaceMemberRepository workspaceMembers;
    @Mock UserRepository users;
    @Mock AttachmentStorage storage;
    AttachmentService service;
    UUID user=UUID.randomUUID(), tid=UUID.randomUUID(), aid=UUID.randomUUID();
    Project project; Workspace workspace; Todo todo; TodoAttachment attachment; User uploader;
    @BeforeEach void setup() {
        service=new AttachmentService(attachments,projects,todos,members,workspaceMembers,users,storage);
        workspace=new Workspace(); workspace.setId(UUID.randomUUID());
        project=new Project(); project.setId(UUID.randomUUID()); project.setWorkspace(workspace);
        todo=new Todo(); todo.setId(tid);
        uploader=new User(); uploader.setId(user); uploader.setName("Uploader");
        attachment=new TodoAttachment(); attachment.setId(aid); attachment.setTodo(todo);
        attachment.setUploadedBy(uploader); attachment.setStorageKey(UUID.randomUUID().toString());
        attachment.setFileName("hello.txt"); attachment.setSizeBytes(5);
        when(projects.findByWorkspace_SlugAndSlug("ws","p")).thenReturn(Optional.of(project));
    }
    void access() { when(members.existsByProject_IdAndUser_Id(project.getId(),user)).thenReturn(true); }
    void lookup(boolean lock) {
        access();
        if(lock) when(todos.findForCommentWrite(tid,project.getId())).thenReturn(Optional.of(todo));
        else when(todos.findByIdAndProject_Id(tid,project.getId())).thenReturn(Optional.of(todo));
    }
    MockMultipartFile file(String name, String body) { return new MockMultipartFile("file",name,"text/plain",body.getBytes()); }
    @Test void outsidersCannotListOrDownload() {
        assertThrows(ForbiddenException.class,()->service.list("ws","p",tid,user));
        assertThrows(ForbiddenException.class,()->service.download("ws","p",tid,aid,user));
        verifyNoInteractions(storage);
    }
    @Test void crossTodoDownloadAndDeleteAreNotFound() {
        lookup(false);
        assertThrows(ResourceNotFoundException.class,()->service.download("ws","p",tid,aid,user));
        when(todos.findForCommentWrite(tid,project.getId())).thenReturn(Optional.of(todo));
        assertThrows(ResourceNotFoundException.class,()->service.delete("ws","p",tid,aid,user));
        verifyNoInteractions(storage);
    }
    @Test void crossProjectTodoNotFound() {
        access();
        assertThrows(ResourceNotFoundException.class,()->service.list("ws","p",tid,user));
        verifyNoInteractions(attachments,storage);
    }
    @Test void uploaderDeleteMarksForCleanupWithoutRemovingFile() {
        lookup(true);
        when(attachments.findByIdAndTodo_IdAndDeletedFalse(aid,tid)).thenReturn(Optional.of(attachment));
        service.delete("ws","p",tid,aid,user);
        assertTrue(attachment.isDeleted()); verify(attachments).saveAndFlush(attachment);
        verifyNoInteractions(storage);
    }
    @Test void otherMemberCannotDelete() {
        lookup(true); uploader.setId(UUID.randomUUID());
        when(attachments.findByIdAndTodo_IdAndDeletedFalse(aid,tid)).thenReturn(Optional.of(attachment));
        assertThrows(ForbiddenException.class,()->service.delete("ws","p",tid,aid,user));
        assertFalse(attachment.isDeleted());
    }
    @Test void projectAdminCanDelete() {
        lookup(true); uploader.setId(UUID.randomUUID());
        when(attachments.findByIdAndTodo_IdAndDeletedFalse(aid,tid)).thenReturn(Optional.of(attachment));
        ProjectRole role=new ProjectRole(); role.setAdmin(true);
        ProjectMember member=new ProjectMember(); member.setProjectRole(role);
        when(members.findByProject_IdAndUser_Id(project.getId(),user)).thenReturn(Optional.of(member));
        service.delete("ws","p",tid,aid,user); assertTrue(attachment.isDeleted());
    }
    @Test void superAdminMustBeProjectMemberToUpload() {
        WorkspaceMember member=new WorkspaceMember(); member.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        when(workspaceMembers.findByWorkspace_IdAndUser_Id(workspace.getId(),user)).thenReturn(Optional.of(member));
        assertThrows(ForbiddenException.class,()->service.upload("ws","p",tid,file("a","a"),user));
    }
    @Test void uploadEnforcesWorkspaceLimitAndNonemptyFile() {
        lookup(true); workspace.setAttachmentMaxBytes(2L);
        assertThrows(AttachmentTooLargeException.class,()->service.upload("ws","p",tid,file("a","abc"),user));
        assertThrows(BadRequestException.class,()->service.upload("ws","p",tid,file("a",""),user));
        verifyNoInteractions(storage);
    }
    @Test void uploadUsesGeneratedKeyAndCleansUpOnRollback() throws Exception {
        lookup(true);
        when(users.getReferenceById(user)).thenReturn(uploader);
        when(storage.write(anyString(),any(),anyLong())).thenReturn(5L);
        when(attachments.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
        try {
            var dto=service.upload("ws","p",tid,file("../../hello.txt","hello"),user);
            assertEquals("hello.txt",dto.fileName()); assertEquals(5,dto.sizeBytes());
            var capture=ArgumentCaptor.forClass(TodoAttachment.class);
            verify(attachments).saveAndFlush(capture.capture());
            String key=capture.getValue().getStorageKey();
            assertDoesNotThrow(()->UUID.fromString(key));
            var sync=TransactionSynchronizationManager.getSynchronizations().get(0);
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            verify(storage,never()).delete(anyString());
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(storage).delete(key);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void validDownloadOpensOnlyStoredKey() throws Exception {
        lookup(false);
        when(attachments.findByIdAndTodo_IdAndDeletedFalse(aid,tid)).thenReturn(Optional.of(attachment));
        when(storage.open(attachment.getStorageKey())).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        try(var stream=service.download("ws","p",tid,aid,user).stream()) {
            assertEquals("hello",new String(stream.readAllBytes()));
        }
    }
}
