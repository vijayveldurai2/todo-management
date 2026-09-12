package com.vijay.todo_management.storage;
import com.vijay.todo_management.exception.AttachmentTooLargeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class LocalAttachmentStorage implements AttachmentStorage {
    private final Path root;
    public LocalAttachmentStorage(@Value("${app.attachments.directory:./data/attachments}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    private Path path(String key) {
        if (key == null || !key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("Invalid storage key");
        return root.resolve(key);
    }
    @Override public long write(String key, InputStream input, long limit) throws IOException {
        Files.createDirectories(root);
        Path target = path(key);
        // CREATE_NEW prevents collisions and does not follow an existing symlink.
        boolean created = false;
        try {
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                byte[] buffer = new byte[8192];
                long total = 0;
                for (int n; (n = input.read(buffer)) != -1;) {
                    total += n;
                    if (total > limit) throw new AttachmentTooLargeException("File exceeds the workspace upload limit");
                    out.write(buffer, 0, n);
                }
                return total;
            }
        } catch (IOException | RuntimeException ex) {
            if (created) {
                try { Files.deleteIfExists(target); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            }
            throw ex;
        }
    }
    @Override public InputStream open(String key) throws IOException {
        return Files.newInputStream(path(key), LinkOption.NOFOLLOW_LINKS);
    }
    @Override public void delete(String key) throws IOException { Files.deleteIfExists(path(key)); }
    @Override public List<String> oldKeys() throws IOException {
        if (!Files.exists(root)) return List.of();
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        try (var paths = Files.list(root)) {
            return paths.filter(p -> p.getFileName().toString().matches("[0-9a-f-]{36}"))
                    .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> {
                        try { return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff); }
                        catch (IOException ex) { return false; }
                    }).map(p -> p.getFileName().toString()).toList();
        }
    }
}

