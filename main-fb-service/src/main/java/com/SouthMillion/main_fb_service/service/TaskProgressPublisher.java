package com.SouthMillion.main_fb_service.service;

import com.SouthMillion.main_fb_service.service.client.TaskReportFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProgressPublisher {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private TaskReportFeignClient taskReportFeign;

    @Value("${app.kafka.task-progress-topic:task.progress.update}")
    private String taskProgressTopic;

    public boolean publish(String playerId, String taskKey, int delta, String source) {
        if (playerId == null || playerId.isBlank() || taskKey == null || taskKey.isBlank() || delta <= 0) {
            return false;
        }

        Long roleId;
        try {
            roleId = Long.parseLong(playerId);
        } catch (NumberFormatException e) {
            log.warn("[MainFbTaskProgress] Invalid playerId={} for taskKey={}", playerId, taskKey);
            return false;
        }

        if (kafkaTemplate != null) {
            TaskProgressEvent event = TaskProgressEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .roleId(roleId)
                    .taskKey(taskKey)
                    .progressDelta(delta)
                    .source(source)
                    .occurredAt(Instant.now())
                    .build();
            try {
                kafkaTemplate.send(taskProgressTopic, playerId, event);
                log.debug("[MainFbTaskProgress] published topic={} roleId={} taskKey={} delta={}",
                        taskProgressTopic, roleId, taskKey, delta);
                return true;
            } catch (Exception e) {
                log.warn("[MainFbTaskProgress] kafka publish failed roleId={} taskKey={}: {}",
                        roleId, taskKey, e.getMessage());
            }
        }

        if (taskReportFeign == null) {
            return false;
        }

        try {
            taskReportFeign.report(TaskReportReq.builder()
                    .playerId(playerId)
                    .taskKey(taskKey)
                    .progressDelta(delta)
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("[MainFbTaskProgress] direct report fallback failed roleId={} taskKey={}: {}",
                    roleId, taskKey, e.getMessage());
            return false;
        }
    }
}