package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.repository.TaskProgressEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProgressEventCleanupJob {

    private final TaskProgressEventRepository taskProgressEventRepository;

    @Value("${task.progress-event.cleanup-enabled:true}")
    private boolean cleanupEnabled;

    @Value("${task.progress-event.retention-days:30}")
    private long retentionDays;

    @Scheduled(initialDelayString = "${task.progress-event.cleanup-initial-delay-ms:300000}",
            fixedDelayString = "${task.progress-event.cleanup-interval-ms:3600000}")
    public void cleanupOldEvents() {
        if (!cleanupEnabled || retentionDays <= 0) {
            return;
        }

        Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = taskProgressEventRepository.deleteByProcessedAtBefore(threshold);
        if (deleted > 0) {
            log.info("[TaskProgress] Cleanup old dedupe events: deleted={} retentionDays={}", deleted, retentionDays);
        }
    }
}