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

    private static final Map<String, Integer> LEGACY_CONDITION_ALIASES = Map.ofEntries(
        Map.entry("open_box", 3),
        Map.entry("condition_3", 3),
        Map.entry("main_fb_pass_level", 4),
        Map.entry("complete_dungeon", 4),
        Map.entry("get_equip", 6),
        Map.entry("condition_6", 6),
        Map.entry("level_up", 1),
        Map.entry("sell_equip_gold", 18),
        Map.entry("arena_win", 26),
        Map.entry("sell_equip_num", 53)
    );

    /**
     * Conditions that report a current-state value (level/floor) should not be summed.
     */
    private static final Map<Integer, ProgressMode> CONDITION_PROGRESS_MODES = Map.ofEntries(
        Map.entry(1, ProgressMode.SNAPSHOT_MAX),
        Map.entry(4, ProgressMode.SNAPSHOT_MAX),
        Map.entry(5, ProgressMode.SNAPSHOT_MAX),
        Map.entry(27, ProgressMode.SNAPSHOT_MAX)
    );

    private TaskConditionRegistry() {
    }

    public static Integer resolveConditionType(String reportedTaskKey) {
        if (reportedTaskKey == null || reportedTaskKey.isBlank()) {
            return null;
        }

        Integer alias = LEGACY_CONDITION_ALIASES.get(reportedTaskKey);
        if (alias != null) {
            return alias;
        }

        if (reportedTaskKey.startsWith("condition_")) {
            return tryParseInteger(reportedTaskKey.substring("condition_".length()));
        }

        return tryParseInteger(reportedTaskKey);
    }

    public static ProgressMode resolveProgressMode(TaskDefinitionConfig config) {
        if (config == null || config.legacyConditionType() == null) {
            return ProgressMode.ACCUMULATE;
        }
        return CONDITION_PROGRESS_MODES.getOrDefault(config.legacyConditionType(), ProgressMode.ACCUMULATE);
    }

    private static Integer tryParseInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
