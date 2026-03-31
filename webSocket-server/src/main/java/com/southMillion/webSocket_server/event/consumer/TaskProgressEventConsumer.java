package com.SouthMillion.webSocket_server.event.consumer;

import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.service.PlayerSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.task.TaskStateChangedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskProgressEventConsumer {

    private final PlayerSessionRegistry sessions;
    private final TaskHandler taskHandler;

    @KafkaListener(
            topics = "${app.kafka.task-state-changed-topic:task.state.changed}",
            groupId = "${spring.application.name:websocket-server}",
            concurrency = "${app.kafka.concurrency:3}"
    )
    public void onTaskStateChanged(TaskStateChangedEvent event) {
        if (event == null || event.getRoleId() == null || event.getTaskKey() == null || event.getTaskKey().isBlank()) {
            return;
        }

        var onlineSessions = sessions.sessionsOfRole(event.getRoleId());
        if (onlineSessions.isEmpty()) {
            return;
        }

        log.debug("[Task] State changed event received roleId={} taskKey={} status={} sessions={}",
                event.getRoleId(), event.getTaskKey(), event.getStatus(), onlineSessions.size());

        for (var session : onlineSessions) {
            taskHandler.pushCurrentTaskProgressIfRelevant(session, event.getTaskKey());
        }
    }
}