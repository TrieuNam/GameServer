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
    String nextTaskKey
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
        this(key, name, description, targetValue, goldReward, expReward, itemRewards, null, null, null);
    }
}

