package com.SouthMillion.task_service.consumer;

import com.SouthMillion.task_service.service.TaskDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Single entry point for task progress updates in task-service.
 *
 * Multiple producers can publish normalized TaskProgressEvent messages
 * to this topic (analytics-service, websocket-server, gameplay services).
 *
 * Listens to: task.progress.update
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProgressEventConsumer {

    private final TaskDomainService taskDomainService;

    @KafkaListener(topics = "task.progress.update", groupId = "task-service")
    public void handleTaskProgressUpdate(@Payload TaskProgressEvent event) {
        try {
            if (event == null) {
                log.warn("[TaskProgress] Skip unreadable/invalid payload from topic task.progress.update");
                return;
            }

            log.info("[TaskProgress] Received: eventId={}, roleId={}, taskKey={}, delta={}, source={}",
                    event.getEventId(), event.getRoleId(), event.getTaskKey(),
                    event.getProgressDelta(), event.getSource());

            boolean applied = taskDomainService.reportProgressEvent(event);

            if (applied) {
                log.debug("[TaskProgress] Applied: taskKey={} +{} for player {}",
                        event.getTaskKey(), event.getProgressDelta(), event.getRoleId());
            } else {
                log.debug("[TaskProgress] Ignored duplicate or invalid event: eventId={}", event.getEventId());
            }

        } catch (Exception e) {
            log.error("[TaskProgress] Failed to process event: eventId={}, taskKey={}, error={}",
                    event.getEventId(), event.getTaskKey(), e.getMessage(), e);
        }
    }
}

