package com.SouthMillion.task_service.service;

import java.util.Map;

/**
 * Centralized legacy task condition aliases to keep condition resolution logic in one place.
 */
public final class TaskConditionRegistry {

    public enum ProgressMode {
        ACCUMULATE,
        SNAPSHOT_MAX
    }

    /**
     * Conditions that report a current-state absolute value — use SNAPSHOT_MAX (take the highest seen).
     * The reporter must send the player's current absolute value as delta (not an increment).
     *   - LEVEL_UP: analytics-service forwards newLevel from role.level.up event
     *   - MAIN_FB_PASS: dungeon service sends current floor cleared
     *   - BOX_LEVEL / FORMATION_LEVEL_UP: send current level value
     */
    private static final Map<Integer, ProgressMode> CONDITION_PROGRESS_MODES = Map.ofEntries(
        Map.entry(TaskCondition.LEVEL_UP.id(), ProgressMode.SNAPSHOT_MAX),
        Map.entry(TaskCondition.MAIN_FB_PASS.id(), ProgressMode.SNAPSHOT_MAX),
        Map.entry(TaskCondition.BOX_LEVEL.id(), ProgressMode.SNAPSHOT_MAX),
        Map.entry(TaskCondition.FORMATION_LEVEL_UP.id(), ProgressMode.SNAPSHOT_MAX)
    );

    private TaskConditionRegistry() {
    }

    public static Integer resolveConditionType(String reportedTaskKey) {
        TaskCondition condition = TaskCondition.fromTaskKey(reportedTaskKey);
        return condition != null ? condition.id() : null;
    }

    public static ProgressMode resolveProgressMode(TaskDefinitionConfig config) {
        if (config == null) {
            return ProgressMode.ACCUMULATE;
        }
        // Explicit config override takes precedence over condition-type default.
        // Set progressMode in task_cfg.json to switch without code changes.
        if (config.progressMode() != null) {
            return config.progressMode();
        }
        if (config.legacyConditionType() == null) {
            return ProgressMode.ACCUMULATE;
        }
        return CONDITION_PROGRESS_MODES.getOrDefault(config.legacyConditionType(), ProgressMode.ACCUMULATE);
    }
}
