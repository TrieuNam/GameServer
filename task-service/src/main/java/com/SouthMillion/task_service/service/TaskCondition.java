package com.SouthMillion.task_service.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Canonical legacy task conditions from task_cfg_auto.json.
 */
public enum TaskCondition {
    LEVEL_UP(1, "level_up"),
    OPEN_BOX(3, "open_box"),
    MAIN_FB_PASS(4, "main_fb_pass_level", "complete_dungeon"),
    BOX_LEVEL(5),
    GET_EQUIP(6, "get_equip"),
    SELL_EQUIP_GOLD(18, "sell_equip_gold"),
    EQUIP_GREEN(19, 2),
    EQUIP_BLUE(20, 3),
    EQUIP_PURPLE(21, 4),
    ARENA_WIN(26, "arena_win"),
    FORMATION_LEVEL_UP(27),
    ESCORT_COMPLETE(29),
    GEM_INLAY(32),
    STARMAP_ACTIVATE(35),
    TRIAL_COMPLETE(37),
    BOX_UPGRADE_ACCUMULATE(38),
    PAGODA_GUMO_CHALLENGE(40),
    TERRITORY_MINE(45),
    PET_LEVEL_UP(47),
    PET_GEM_UPGRADE(48),
    SELL_EQUIP_NUM(53, "sell_equip_num"),
    SHIZHUANG_FUMO(56),
    SHENQI_DRAW(57),
    TERRITORY_BUY_BOT(58),
    TERRITORY_FETCH_REWARD(59),
    LINGZHU_CHALLENGE(60),
    RUNE_EQUIP(61),
    TERRITORY_REFRESH_NEIGHBOUR(63),
    BLOCK_RECEIVE(70),
    BLOCK_INLAY(71),
    BLOCK_DESIGN_CHANGE(72),
    BLOCK_COMPOSE(73);

    private static final Map<Integer, TaskCondition> BY_ID;
    private static final Map<String, TaskCondition> BY_ALIAS;

    static {
        Map<Integer, TaskCondition> byId = new LinkedHashMap<>();
        Map<String, TaskCondition> byAlias = new HashMap<>();
        for (TaskCondition condition : values()) {
            byId.put(condition.id, condition);
            byAlias.put(condition.key(), condition);
            byAlias.put(String.valueOf(condition.id), condition);
            for (String alias : condition.aliases) {
                byAlias.put(alias, condition);
            }
        }
        BY_ID = Map.copyOf(byId);
        BY_ALIAS = Map.copyOf(byAlias);
    }

    private final int id;
    private final Integer minQuality;
    private final String preferredTaskKey;
    private final Set<String> aliases;

    TaskCondition(int id, String... aliases) {
        this(id, null, aliases);
    }

    TaskCondition(int id, Integer minQuality, String... aliases) {
        this.id = id;
        this.minQuality = minQuality;
        List<String> normalizedAliases = aliases == null
                ? List.of()
                : Arrays.stream(aliases)
                        .filter(alias -> alias != null && !alias.isBlank())
                        .map(TaskCondition::normalize)
                        .toList();
        this.preferredTaskKey = normalizedAliases.isEmpty() ? key() : normalizedAliases.get(0);
        this.aliases = Set.copyOf(normalizedAliases);
    }

    public int id() {
        return id;
    }

    public Integer minQuality() {
        return minQuality;
    }

    public String key() {
        return "condition_" + id;
    }

    public String taskKey() {
        return preferredTaskKey;
    }

    public static TaskCondition fromId(Integer id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static TaskCondition fromTaskKey(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return null;
        }
        String normalized = normalize(taskKey);
        TaskCondition aliasMatch = BY_ALIAS.get(normalized);
        if (aliasMatch != null) {
            return aliasMatch;
        }
        if (normalized.startsWith("condition_")) {
            return fromId(parseInteger(normalized.substring("condition_".length())));
        }
        return fromId(parseInteger(normalized));
    }

    public static List<TaskCondition> equipQualityConditions(int quality) {
        if (quality <= 0) {
            return List.of();
        }
        return Arrays.stream(values())
                .filter(condition -> condition.minQuality != null)
                .filter(condition -> quality >= condition.minQuality)
                .sorted(Comparator.comparing(TaskCondition::minQuality))
                .toList();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
