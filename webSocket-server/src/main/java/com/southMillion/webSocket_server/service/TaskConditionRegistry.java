package com.SouthMillion.webSocket_server.service;

import java.util.Map;

/**
 * Shared legacy condition-key aliases for websocket-side task matching and refresh decisions.
 */
public final class TaskConditionRegistry {

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

    private TaskConditionRegistry() {
    }

    public static Integer resolveConditionType(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return null;
        }

        Integer alias = LEGACY_CONDITION_ALIASES.get(taskKey);
        if (alias != null) {
            return alias;
        }

        if (taskKey.startsWith("condition_")) {
            return parseInteger(taskKey.substring("condition_".length()));
        }

        return parseInteger(taskKey);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
