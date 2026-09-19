package com.vijay.todo_management.service;
import com.vijay.todo_management.entity.TodoAttachment;
import com.vijay.todo_management.repository.AttachmentRepository;
import com.vijay.todo_management.storage.AttachmentStorage;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import static org.mockito.Mockito.*;
class AttachmentCleanupTest {
    @Test void failedFileDeletionRetainsRetryRecord() throws Exception {
        var repo=mock(AttachmentRepository.class); var storage=mock(AttachmentStorage.class);
        var a=new TodoAttachment(); a.setId(UUID.randomUUID()); a.setStorageKey("key");
        when(repo.findCleanupCandidates(any())).thenReturn(List.of(a));
        doThrow(new IOException("locked")).when(storage).delete("key");
        new AttachmentCleanup(repo,storage).clean();
        verify(repo,never()).deleteById(any());
    }
    @Test void successfulCleanupDeletesFileBeforeMetadata() throws Exception {
        var repo=mock(AttachmentRepository.class); var storage=mock(AttachmentStorage.class);
        var a=new TodoAttachment(); a.setId(UUID.randomUUID()); a.setStorageKey("key");
        when(repo.findCleanupCandidates(any())).thenReturn(List.of(a));
        new AttachmentCleanup(repo,storage).clean();
        var order=inOrder(repo,storage);
        order.verify(storage).delete("key"); order.verify(repo).deleteById(a.getId());
    }
    @Test void orphanCleanupPreservesReferencedFiles() throws Exception {
        var repo=mock(AttachmentRepository.class); var storage=mock(AttachmentStorage.class);
        when(storage.oldKeys()).thenReturn(List.of("live","orphan"));
        when(repo.existsByStorageKey("live")).thenReturn(true);
        new AttachmentCleanup(repo,storage).cleanOrphans();
        verify(storage).delete("orphan"); verify(storage,never()).delete("live");
    }
}
