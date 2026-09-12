package com.vijay.todo_management.service;
import com.vijay.todo_management.repository.AttachmentRepository;
import com.vijay.todo_management.storage.AttachmentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component @EnableScheduling @RequiredArgsConstructor @Slf4j
public class AttachmentCleanup {
    private final AttachmentRepository repository;
    private final AttachmentStorage storage;
    @Scheduled(fixedDelayString = "${app.attachments.cleanup-delay-ms:60000}")
    public void clean() {
        // Tombstones survive restarts. Remove metadata only after physical deletion succeeds.
        for (var attachment : repository.findCleanupCandidates(PageRequest.of(0, 100))) {
            try {
                storage.delete(attachment.getStorageKey());
                repository.deleteById(attachment.getId());
            } catch (Exception ex) {
                log.warn("Attachment cleanup will retry {}", attachment.getId());
            }
        }
    }
    @Scheduled(fixedDelayString = "${app.attachments.orphan-cleanup-delay-ms:3600000}")
    public void cleanOrphans() {
        // Grace period covers normal in-flight uploads; keys are never reused.
        try {
            for (String key : storage.oldKeys()) {
                if (!repository.existsByStorageKey(key)) storage.delete(key);
            }
        } catch (Exception ex) {
            log.warn("Orphan attachment cleanup will retry");
        }
    }
}

