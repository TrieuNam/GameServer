package com.SouthMillion.webSocket_server.service;

/**
 * Shared legacy condition-key aliases for websocket-side task matching and refresh decisions.
 */
public final class TaskConditionRegistry {

    private TaskConditionRegistry() {
    }

    public static Integer resolveConditionType(String taskKey) {
        TaskCondition condition = TaskCondition.fromTaskKey(taskKey);
        return condition != null ? condition.id() : null;
    }
}
