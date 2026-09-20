package com.vijay.todo_management.service;
import com.vijay.todo_management.storage.LocalAttachmentStorage;
import com.vijay.todo_management.exception.AttachmentTooLargeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class LocalAttachmentStorageTest {
    @TempDir Path dir;
    @Test void roundTripAndIdempotentDelete() throws Exception {
        var store=new LocalAttachmentStorage(dir.toString()); String key=UUID.randomUUID().toString();
        assertEquals(5,store.write(key,new ByteArrayInputStream("hello".getBytes()),10));
        try(var in=store.open(key)) { assertEquals("hello",new String(in.readAllBytes())); }
        store.delete(key); store.delete(key); assertFalse(Files.exists(dir.resolve(key)));
    }
    @Test void oversizedStreamRemovesPartialFile() {
        var store=new LocalAttachmentStorage(dir.toString()); String key=UUID.randomUUID().toString();
        assertThrows(AttachmentTooLargeException.class,()->store.write(key,new ByteArrayInputStream(new byte[20]),10));
        assertFalse(Files.exists(dir.resolve(key)));
    }
    @Test void rejectTraversalAndDoNotOverwriteExistingFile() throws Exception {
        var store=new LocalAttachmentStorage(dir.toString());
        assertThrows(IllegalArgumentException.class,()->store.open("../secret"));
        String key=UUID.randomUUID().toString(); Files.writeString(dir.resolve(key),"original");
        assertThrows(FileAlreadyExistsException.class,()->store.write(key,new ByteArrayInputStream(new byte[1]),10));
        assertEquals("original",Files.readString(dir.resolve(key)));
    }
    @Test void orphanScanOnlyIncludesOldGeneratedFiles() throws Exception {
        var store=new LocalAttachmentStorage(dir.toString());
        String old=UUID.randomUUID().toString(), fresh=UUID.randomUUID().toString();
        Files.writeString(dir.resolve(old),"old"); Files.writeString(dir.resolve(fresh),"new");
        Files.setLastModifiedTime(dir.resolve(old),FileTime.from(Instant.now().minusSeconds(172800)));
        Files.writeString(dir.resolve("unrelated.txt"),"ignore");
        assertEquals(java.util.List.of(old),store.oldKeys());
    }
}
