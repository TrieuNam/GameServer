package com.SouthMillion.task_service.service;

public record TaskDefinitionConfig(
    String key,
    String name,
    String description,
    int targetValue,
    int goldReward,
    int expReward,
    String itemRewards,
    Integer legacyConditionType,
    Integer legacyParam1,
    String nextTaskKey,
    /**
     * Explicit progress mode override. When set, takes precedence over the condition-type default.
     * Use "SNAPSHOT_MAX" when the reporter sends the player's absolute value (e.g. current level).
     * Use "ACCUMULATE" when the reporter sends increments (e.g. delta=1 per event).
     * Leave null to use the condition-type default from TaskConditionRegistry.
     */
    TaskConditionRegistry.ProgressMode progressMode
) {

    public TaskDefinitionConfig(
        String key,
        String name,
        String description,
        int targetValue,
        int goldReward,
        int expReward,
        String itemRewards
    ) {
        this(key, name, description, targetValue, goldReward, expReward, itemRewards, null, null, null, null);
    }
}

