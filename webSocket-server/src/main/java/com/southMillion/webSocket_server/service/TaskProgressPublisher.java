package com.SouthMillion.webSocket_server.service;

import com.SouthMillion.webSocket_server.service.client.TaskFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProgressPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TaskFeign taskFeign;

    @Value("${app.kafka.task-progress-topic:task.progress.update}")
    private String taskProgressTopic;

    public boolean publish(Long roleId, String taskKey, int delta, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank() || delta <= 0) {
            return false;
        }

        String roleKey = String.valueOf(roleId);
        TaskProgressEvent event = TaskProgressEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .roleId(roleId)
                .taskKey(taskKey)
                .progressDelta(delta)
                .source(source)
                .occurredAt(Instant.now())
                .build();

        try {
            kafkaTemplate.send(taskProgressTopic, roleKey, event);
            log.debug("[TaskProgressPublisher] published topic={} roleId={} taskKey={} delta={} source={}",
                    taskProgressTopic, roleId, taskKey, delta, source);
            return true;
        } catch (Exception e) {
            log.warn("[TaskProgressPublisher] kafka publish failed roleId={} taskKey={} source={}: {}. Falling back to direct report.",
                    roleId, taskKey, source, e.getMessage());
        }

        try {
            taskFeign.reportProgress(TaskReportReq.builder()
                    .playerId(roleKey)
                    .taskKey(taskKey)
                    .progressDelta(delta)
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("[TaskProgressPublisher] direct report fallback failed roleId={} taskKey={} source={}: {}",
                    roleId, taskKey, source, e.getMessage());
            return false;
        }
    }
}