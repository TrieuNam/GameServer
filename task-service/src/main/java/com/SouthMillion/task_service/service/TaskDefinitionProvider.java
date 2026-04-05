package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.client.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task definition provider with Redis-first lookup strategy.
 * 1. Check Redis cache first
 * 2. If miss, call config-service
 * 3. Cache result in Redis for future requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDefinitionProvider {

    private static final Map<String, TaskDefinitionConfig> FALLBACK_TASK_CONFIGS = Map.of(
        "daily_login",    new TaskDefinitionConfig("daily_login",    "Dang nhap hang ngay",    "Dang nhap vao game",         1,    100,  50,  ""),
        "kill_monster",   new TaskDefinitionConfig("kill_monster",   "Tieu diet quai vat",     "Tieu diet 10 quai vat",      10,   200,  100, "item:101:5"),
        "complete_dungeon",new TaskDefinitionConfig("complete_dungeon","Hoan thanh pho ban",   "Hoan thanh 3 pho ban",       3,    500,  200, "item:102:10"),
        "level_up",       new TaskDefinitionConfig("level_up",       "Nang cap",              "Dat level 10",               10,   1000, 500, "item:103:1"),
        "spend_gold",     new TaskDefinitionConfig("spend_gold",     "Chi tieu vang",          "Chi tieu 1000 vang",         1000, 50,   100, ""),
        "join_guild",     new TaskDefinitionConfig("join_guild",     "Gia nhap bang hoi",      "Gia nhap hoac tao bang hoi", 1,    300,  150, ""),
        "create_guild",   new TaskDefinitionConfig("create_guild",   "Tao bang hoi",           "Tao mot bang hoi moi",       1,    500,  200, "item:104:1"),
        "arena_win",      new TaskDefinitionConfig("arena_win",      "Chien thang dau truong", "Thang 10 tran dau truong",   10,   300,  150, ""),
        "trial_complete", new TaskDefinitionConfig("trial_complete", "Hoan thanh ai",          "Hoan thanh 5 ai thu thach",  5,    200,  100, "")
    );

    private final ConfigServiceClient configServiceClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;

    @Value("${task.config.path:gameworld/logicconfig/task_cfg.json}")
    private String taskConfigPath;

    @Value("${task.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${task.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    private final AtomicReference<Map<String, TaskDefinitionConfig>> cacheRef =
        new AtomicReference<>(FALLBACK_TASK_CONFIGS);
    private final AtomicReference<String> etagRef = new AtomicReference<>(null);
    private final AtomicReference<Instant> lastRefreshAtRef = new AtomicReference<>(null);
    private final AtomicReference<Instant> lastSuccessAtRef = new AtomicReference<>(null);
    private final AtomicReference<String> lastSourceRef = new AtomicReference<>("fallback");
    private final AtomicReference<String> lastErrorRef = new AtomicReference<>(null);
    // Guard: ContextRefreshedEvent fires multiple times (Feign child contexts, Eureka re-register, etc.)
    private final java.util.concurrent.atomic.AtomicBoolean warmedUp = new java.util.concurrent.atomic.AtomicBoolean(false);

    @EventListener(ContextRefreshedEvent.class)
    public void warmUp() {
        if (warmedUp.compareAndSet(false, true)) {
            refreshFromRemote(false);
        }
    }

    @Scheduled(
        initialDelayString = "${task.config.refresh-initial-delay-ms:60000}",
        fixedDelayString = "${task.config.refresh-interval-ms:60000}"
    )
    public void refresh() {
        refreshFromRemote(false);
    }

    public TaskConfigStatus manualReload() {
        refreshFromRemote(true);
        return status();
    }

    public TaskConfigStatus status() {
        return new TaskConfigStatus(
            taskConfigPath,
            cacheRef.get().size(),
            etagRef.get(),
            lastRefreshAtRef.get(),
            lastSuccessAtRef.get(),
            lastSourceRef.get(),
            lastErrorRef.get()
        );
    }

    public Map<String, TaskDefinitionConfig> getTaskConfigs() {
        return cacheRef.get();
    }

    public TaskDefinitionConfig getTaskConfig(String taskKey) {
        return cacheRef.get().get(taskKey);
    }

    private void refreshFromRemote(boolean force) {
        lastRefreshAtRef.set(Instant.now());

        // 1. Try Redis first (if enabled and not force reload)
        if (redisEnabled && !force) {
            String redisKey = toRedisKey(taskConfigPath);
            String cached = redis.opsForValue().get(redisKey);
            if (cached != null && !cached.isBlank()) {
                log.debug("[TaskDefinitionProvider] Redis HIT path={}", taskConfigPath);
                try {
                    Map<String, TaskDefinitionConfig> parsedConfigs = parseConfigs(cached);
                    if (!parsedConfigs.isEmpty()) {
                        cacheRef.set(Map.copyOf(parsedConfigs));
                        lastSuccessAtRef.set(Instant.now());
                        lastSourceRef.set("redis");
                        lastErrorRef.set(null);
                        touchRedisKey(redisKey);
                        return;
                    }
                    redis.delete(redisKey);
                } catch (Exception e) {
                    log.warn("Failed to parse cached task config from Redis, will reload: {}", e.toString());
                    try {
                        redis.delete(redisKey);
                    } catch (Exception ignored) {
                        // ignore invalid-cache cleanup failure
                    }
                }
            }
            log.debug("[TaskDefinitionProvider] Redis MISS path={}", taskConfigPath);
        }

        // 2. Call config-service
        try {
            String ifNoneMatch = force ? null : etagRef.get();
            ResponseEntity<byte[]> response = configServiceClient.getFile(taskConfigPath, ifNoneMatch);
            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                lastSourceRef.set("cache");
                lastErrorRef.set(null);
                return;
            }

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Task config refresh skipped, status={}", response.getStatusCode());
                lastErrorRef.set("refresh skipped: status=" + response.getStatusCode());
                return;
            }

            String payload = new String(response.getBody(), StandardCharsets.UTF_8);
            Map<String, TaskDefinitionConfig> parsedConfigs = parseConfigs(payload);
            if (parsedConfigs.isEmpty()) {
                log.warn("Task config payload parsed empty, keeping current cache");
                return;
            }

            cacheRef.set(Map.copyOf(parsedConfigs));
            String etag = response.getHeaders().getFirst(HttpHeaders.ETAG);
            if (StringUtils.hasText(etag)) {
                etagRef.set(etag);
            }
            lastSuccessAtRef.set(Instant.now());
            lastSourceRef.set("config-service");
            lastErrorRef.set(null);

            // 3. Cache in Redis
            if (redisEnabled) {
                String redisKey = toRedisKey(taskConfigPath);
                redis.opsForValue().set(redisKey, payload, redisTtlHours, TimeUnit.HOURS);
                log.debug("[TaskDefinitionProvider] Cached in Redis path={}", taskConfigPath);
            }

            log.info("Loaded {} task definitions from config-service (path={})", parsedConfigs.size(), taskConfigPath);
        } catch (FeignException.NotFound ex) {
            log.warn("Task config path not found on config-service: {}", taskConfigPath);
            lastErrorRef.set("not found: " + taskConfigPath);
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                lastSourceRef.set("cache");
                lastErrorRef.set(null);
                return;
            }
            log.warn("Task config refresh failed via Feign, status={}, message={}", ex.status(), ex.getMessage());
            lastErrorRef.set("feign status=" + ex.status());
        } catch (Exception ex) {
            log.warn("Task config refresh failed: {}", ex.getMessage());
            lastErrorRef.set(ex.getMessage());
        }
    }

    private Map<String, TaskDefinitionConfig> parseConfigs(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        Map<String, TaskDefinitionConfig> out = new LinkedHashMap<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                addConfig(node, out);
            }
            return out;
        }

        JsonNode tasksNode = firstExisting(root, List.of("task_list", "tasks", "data", "items"));
        if (tasksNode != null && tasksNode.isArray()) {
            for (JsonNode node : tasksNode) {
                addConfig(node, out);
            }
            return out;
        }

        // Fallback format: { "daily_login": { ... }, "kill_monster": { ... } }
        root.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject()) {
                addConfig(entry.getValue(), out, entry.getKey());
            }
        });

        return out;
    }

    private JsonNode firstExisting(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private void addConfig(JsonNode node, Map<String, TaskDefinitionConfig> out) {
        addConfig(node, out, null);
    }

    private void addConfig(JsonNode node, Map<String, TaskDefinitionConfig> out, String fallbackKey) {
        String key = text(node, "taskKey", "task_key", "task_id", "key", "id");
        if (!StringUtils.hasText(key) && StringUtils.hasText(fallbackKey)) {
            key = fallbackKey;
        }
        if (!StringUtils.hasText(key)) {
            return;
        }

        String name = text(node, "taskName", "task_name", "name", "title");
        String description = text(node, "description", "desc");
        int targetValue = number(node, 1, "targetValue", "target_value", "target", "goal", "param");
        int goldReward = number(node, 0, "goldReward", "gold_reward", "rewardGold", "gold");
        int expReward = number(node, 0, "expReward", "exp_reward", "rewardExp", "exp");
        Integer legacyConditionType = optionalNumber(node, "condition", "condition_type");
        Integer legacyParam1 = optionalNumber(node, "param_1", "param1");
        String nextTaskKey = text(node, "next_task_id", "nextTaskId", "next_task_key", "nextTaskKey");
        String itemRewards = text(node, "itemRewards", "item_rewards", "rewards", "itemReward");
        if (!StringUtils.hasText(itemRewards)) {
            itemRewards = parseRewardArray(node.get("reward"));
        }
        TaskConditionRegistry.ProgressMode progressMode = parseProgressMode(
            text(node, "progressMode", "progress_mode"));

        out.put(key, new TaskDefinitionConfig(
            key,
            defaultIfBlank(name, key),
            defaultIfBlank(description, ""),
            Math.max(targetValue, 1),
            Math.max(goldReward, 0),
            Math.max(expReward, 0),
            defaultIfBlank(itemRewards, ""),
            legacyConditionType,
            legacyParam1,
            defaultIfBlank(nextTaskKey, null),
            progressMode
        ));
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private int number(JsonNode node, int fallback, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.intValue();
            }
            if (value.isTextual()) {
                try {
                    return Integer.parseInt(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // Skip invalid format and continue checking other aliases.
                }
            }
        }
        return fallback;
    }

    private Integer optionalNumber(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.intValue();
            }
            if (value.isTextual()) {
                String text = value.asText().trim();
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    // Ignore invalid optional numeric fields.
                }
            }
        }
        return null;
    }

    private String parseRewardArray(JsonNode rewardNode) {
        if (rewardNode == null || rewardNode.isNull() || !rewardNode.isArray()) {
            return null;
        }

        List<String> chunks = new ArrayList<>();
        for (JsonNode item : rewardNode) {
            String itemId = text(item, "item_id", "itemId", "id");
            int num = number(item, 0, "num", "count", "quantity");
            if (StringUtils.hasText(itemId) && num > 0) {
                chunks.add("item:" + itemId + ":" + num);
            }
        }

        if (chunks.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(",");
        chunks.forEach(joiner::add);
        return joiner.toString();
    }

    private TaskConditionRegistry.ProgressMode parseProgressMode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return TaskConditionRegistry.ProgressMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("[TaskDefinitionProvider] Unknown progressMode '{}', ignored", value);
            return null;
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[TaskDefinitionProvider] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    public record TaskConfigStatus(
        String path,
        int taskCount,
        String etag,
        Instant lastRefreshAt,
        Instant lastSuccessAt,
        String source,
        String lastError
    ) {
    }
}
