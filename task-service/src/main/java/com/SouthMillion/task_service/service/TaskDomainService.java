package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.client.BagClient;
import com.SouthMillion.task_service.client.ItemMetaClient;
import com.SouthMillion.task_service.client.WalletClient;
import com.SouthMillion.task_service.entity.TaskProgressEventEntity;
import com.SouthMillion.task_service.entity.TaskProgressEntity;
import com.SouthMillion.task_service.repository.TaskProgressEventRepository;
import com.SouthMillion.task_service.repository.TaskProgressRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.task.TaskStateChangedEvent;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.task.*;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskDomainService {

    private static final String TASK_STATE_CHANGED_TOPIC = "task.state.changed";
    private static final Map<String, TaskDefinitionConfig> LOCAL_FALLBACK_TASK_CONFIGS = Map.of(
        "daily_login",   new TaskDefinitionConfig("daily_login",   "Dang nhap hang ngay",    "Dang nhap vao game",           1,    100,  50,  ""),
        "kill_monster",  new TaskDefinitionConfig("kill_monster",  "Tieu diet quai vat",     "Tieu diet 10 quai vat",        10,   200,  100, "item:101:5"),
        "complete_dungeon", new TaskDefinitionConfig("complete_dungeon", "Hoan thanh pho ban","Hoan thanh 3 pho ban",         3,    500,  200, "item:102:10"),
        "level_up",      new TaskDefinitionConfig("level_up",      "Nang cap",               "Dat level 10",                 10,   1000, 500, "item:103:1"),
        "spend_gold",    new TaskDefinitionConfig("spend_gold",    "Chi tieu vang",          "Chi tieu 1000 vang",           1000, 50,   100, ""),
        "join_guild",    new TaskDefinitionConfig("join_guild",    "Gia nhap bang hoi",      "Gia nhap hoac tao bang hoi",   1,    300,  150, ""),
        "create_guild",  new TaskDefinitionConfig("create_guild",  "Tao bang hoi",           "Tao mot bang hoi moi",         1,    500,  200, "item:104:1"),
        "arena_win",     new TaskDefinitionConfig("arena_win",     "Chien thang dau truong", "Thang 10 tran dau truong",     10,   300,  150, ""),
        "trial_complete",new TaskDefinitionConfig("trial_complete","Hoan thanh ai",          "Hoan thanh 5 ai thu thach",    5,    200,  100, "")
    );

    private final TaskProgressRepository taskProgressRepository;
    private final TaskProgressEventRepository taskProgressEventRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;
    private final TaskDefinitionProvider taskDefinitionProvider;

    // Virtual Thread executor for parallel operations
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Metrics
    private final Timer claimRewardTimer;
    private final Counter claimRewardSuccessCounter;
    private final Counter claimRewardFailureCounter;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private ItemMetaClient itemMetaClient;

    public TaskDomainService(TaskProgressRepository taskProgressRepository,
                             TaskProgressEventRepository taskProgressEventRepository,
                             WalletClient walletClient,
                             BagClient bagClient,
                             TaskDefinitionProvider taskDefinitionProvider,
                             MeterRegistry meterRegistry) {
        this.taskProgressRepository = taskProgressRepository;
        this.taskProgressEventRepository = taskProgressEventRepository;
        this.walletClient = walletClient;
        this.bagClient = bagClient;
        this.taskDefinitionProvider = taskDefinitionProvider;

        // Initialize metrics
        this.claimRewardTimer = Timer.builder("task.claim.duration")
                .description("Task claim reward operation duration")
                .tag("service", "task-service")
                .register(meterRegistry);
        this.claimRewardSuccessCounter = meterRegistry.counter("task.claim.success");
        this.claimRewardFailureCounter = meterRegistry.counter("task.claim.failure");
    }

    public TaskListResp getAllTasks(String playerId) {
        log.info("Getting all tasks for player: {}", playerId);
        Map<String, TaskDefinitionConfig> taskConfigs = currentTaskConfigs();

        List<TaskProgressEntity> progressList = taskProgressRepository.findAllByPlayerId(Long.parseLong(playerId));
        Map<String, TaskProgressEntity> progressMap = progressList.stream()
            .collect(Collectors.toMap(TaskProgressEntity::getTaskKey, p -> p));

        List<TaskDTO> tasks = new ArrayList<>();
        int completedCount = 0;
        int claimedCount = 0;

        List<String> sortedKeys = taskConfigs.keySet().stream()
            .sorted(taskKeyComparator())
            .toList();

        for (String taskKey : sortedKeys) {
            TaskDefinitionConfig config = taskConfigs.get(taskKey);

            TaskProgressEntity progress = progressMap.get(taskKey);
            TaskStatus status = TaskStatus.NOT_STARTED;
            int currentProgress = 0;
            Instant lastUpdate = null;

            if (progress != null) {
                currentProgress = progress.getProgressValue();
                status = progress.getStatus();
                lastUpdate = progress.getLastUpdate();

                if (status == TaskStatus.COMPLETED || status == TaskStatus.CLAIMED) {
                    completedCount++;
                }
                if (status == TaskStatus.CLAIMED) {
                    claimedCount++;
                }
            }

            tasks.add(TaskDTO.builder()
                .taskKey(taskKey)
                .taskName(config.name())
                .description(config.description())
                .targetValue(config.targetValue())
                .currentProgress(currentProgress)
                .legacyConditionType(config.legacyConditionType())
                .status(status)
                .lastUpdate(lastUpdate)
                .goldReward(config.goldReward())
                .expReward(config.expReward())
                .itemRewards(config.itemRewards())
                .build());
        }

        return TaskListResp.builder()
            .tasks(tasks)
            .totalTasks(taskConfigs.size())
            .completedTasks(completedCount)
            .claimedTasks(claimedCount)
            .build();
    }

    @Transactional
    public boolean reportProgressEvent(TaskProgressEvent event) {
        if (event == null || event.getRoleId() == null || event.getEventId() == null || event.getEventId().isBlank()) {
            return false;
        }

        try {
            taskProgressEventRepository.save(TaskProgressEventEntity.builder()
                    .eventId(event.getEventId())
                    .playerId(event.getRoleId())
                    .taskKey(event.getTaskKey())
                    .source(event.getSource())
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("[TaskProgress] Duplicate event ignored: eventId={}, roleId={}, taskKey={}",
                    event.getEventId(), event.getRoleId(), event.getTaskKey());
            return false;
        }

        reportProgress(TaskReportReq.builder()
                .playerId(String.valueOf(event.getRoleId()))
                .taskKey(event.getTaskKey())
                .progressDelta(event.getProgressDelta())
                .build());
        return true;
    }

    @Transactional
    public void reportProgress(TaskReportReq req) {
        log.info("Reporting progress for player: {}, task: {}, delta: {}",
            req.getPlayerId(), req.getTaskKey(), req.getProgressDelta());

        Map<String, TaskDefinitionConfig> taskConfigs = currentTaskConfigs();
        String targetTaskKey = resolveReportTargetTaskKey(req, taskConfigs);
        TaskDefinitionConfig config = targetTaskKey == null ? null : taskConfigs.get(targetTaskKey);
        if (config == null) {
            log.debug("[TaskResolve] IGNORED player={} reportKey={} → resolvedKey={} not found in taskConfigs (total={} tasks)",
                req.getPlayerId(), req.getTaskKey(), targetTaskKey, taskConfigs.size());
            return;
        }

        int delta = Math.max(req.getProgressDelta() == null ? 0 : req.getProgressDelta(), 0);
        if (delta <= 0) {
            return;
        }

        TaskProgressEntity progress = taskProgressRepository
            .findByPlayerIdAndTaskKey(Long.parseLong(req.getPlayerId()), targetTaskKey)
            .orElseGet(() -> TaskProgressEntity.builder()
                .playerId(Long.parseLong(req.getPlayerId()))
                .taskKey(targetTaskKey)
                .progressValue(0)
                .status(TaskStatus.IN_PROGRESS)
                .lastUpdate(Instant.now())
                .build());

        // Skip if already claimed
        if (progress.getStatus() == TaskStatus.CLAIMED) {
            log.info("Task already claimed: {}", req.getTaskKey());
            return;
        }

        // Update progress by condition mode: counters add delta, state snapshots take max.
        int currentProgress = Math.max(progress.getProgressValue(), 0);
        TaskConditionRegistry.ProgressMode progressMode = TaskConditionRegistry.resolveProgressMode(config);
        int rawProgress = switch (progressMode) {
            case SNAPSHOT_MAX -> Math.max(currentProgress, delta);
            case ACCUMULATE -> currentProgress + delta;
        };
        int newProgress = Math.min(rawProgress, config.targetValue());
        progress.setProgressValue(newProgress);
        progress.setLastUpdate(Instant.now());

        // Check if completed
        if (newProgress >= config.targetValue() && progress.getStatus() != TaskStatus.COMPLETED) {
            progress.setStatus(TaskStatus.COMPLETED);
            log.info("Task completed: {} for player: {}", req.getTaskKey(), req.getPlayerId());
        } else if (progress.getStatus() == TaskStatus.NOT_STARTED) {
            progress.setStatus(TaskStatus.IN_PROGRESS);
        }

        taskProgressRepository.save(progress);
        publishTaskStateChangedAfterCommit(req.getPlayerId(), targetTaskKey, progress, config);
    }

    private void publishTaskStateChangedAfterCommit(
            String playerId,
            String taskKey,
            TaskProgressEntity progress,
            TaskDefinitionConfig config
    ) {
        if (kafkaTemplate == null) {
            return;
        }

        Runnable publishAction = () -> {
            try {
                TaskStateChangedEvent event = TaskStateChangedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .roleId(Long.parseLong(playerId))
                        .taskKey(taskKey)
                        .currentProgress(progress.getProgressValue())
                        .targetValue(config.targetValue())
                        .status(progress.getStatus())
                        .occurredAt(Instant.now())
                        .build();

                kafkaTemplate.send(TASK_STATE_CHANGED_TOPIC, playerId, event);
                log.debug("[Task] Published state change: playerId={} taskKey={} progress={} status={}",
                        playerId, taskKey, progress.getProgressValue(), progress.getStatus());
            } catch (Exception e) {
                log.warn("[Task] Failed to publish state change: playerId={} taskKey={} error={}",
                        playerId, taskKey, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }

    @Transactional
    public void claim(TaskClaimReq req) {
        log.info("Claiming reward for player: {}, task: {}", req.getPlayerId(), req.getTaskKey());

        Long playerId = Long.parseLong(req.getPlayerId());
        TaskProgressEntity progress = taskProgressRepository
            .findByPlayerIdAndTaskKey(playerId, req.getTaskKey())
            .orElseThrow(() -> new IllegalStateException("Task not found: " + req.getTaskKey()));

        if (progress.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task not completed yet: " + req.getTaskKey());
        }

        // Atomic state transition prevents duplicate claim when concurrent requests race.
        int updated = taskProgressRepository.markClaimedIfCompleted(playerId, req.getTaskKey(), Instant.now());
        if (updated <= 0) {
            throw new IllegalStateException("Task claim conflict or already claimed: " + req.getTaskKey());
        }

        // Grant rewards via wallet-service and bag-service
        TaskDefinitionConfig config = findTaskConfig(req.getTaskKey());
        if (config != null) {
            grantRewards(req.getPlayerId(), config);
            log.info("Successfully granted rewards for task {} - Gold: {}, Exp: {}, Items: {}",
                req.getTaskKey(), config.goldReward(), config.expReward(), config.itemRewards());
        }
    }

    /**
     * Advance player to next task in sequence.
     * The current task MUST be COMPLETED before it can be claimed/advanced.
     * Returns the new claimedTasks count (= client's next sequential task_id).
     *
     * Task order: numeric task_id ascending when keys are numeric, with deterministic
     * lexical fallback for legacy non-numeric keys.
     *
     * Used by TaskHandler (msgId 1451) via TaskFeign.
     */
    @Transactional
    public int advanceTask(String playerId) {
        log.info("[Task] advanceTask for player: {}", playerId);
        long pid = Long.parseLong(playerId);

        Map<String, TaskDefinitionConfig> configs = currentTaskConfigs();
        List<String> sortedKeys = configs.keySet().stream()
            .sorted(taskKeyComparator())
            .toList();

        int claimedCount = countClaimedTasks(pid, sortedKeys);
        String taskKey = findCurrentTaskKey(pid, sortedKeys);
        if (taskKey == null) {
            log.info("[Task] All {} tasks already claimed for player: {}", sortedKeys.size(), playerId);
            return claimedCount; // nothing more to advance
        }
        TaskDefinitionConfig config = configs.get(taskKey);

        TaskProgressEntity progress = taskProgressRepository
            .findByPlayerIdAndTaskKey(pid, taskKey)
            .orElse(null);

        // Task must be COMPLETED before it can be advanced
        if (progress == null || progress.getStatus() != TaskStatus.COMPLETED) {
            TaskStatus currentStatus = progress == null ? TaskStatus.NOT_STARTED : progress.getStatus();
            log.warn("[Task] Player {} cannot advance task '{}' — current status: {}. Task must be COMPLETED first.",
                playerId, taskKey, currentStatus);
            return claimedCount; // return current index without advancing
        }

        // Atomic state transition prevents duplicate claim when concurrent requests race.
        int updated = taskProgressRepository.markClaimedIfCompleted(pid, taskKey, Instant.now());
        if (updated <= 0) {
            log.warn("[Task] Player {} advance conflict on task '{}' — state changed concurrently.", playerId, taskKey);
            return countClaimedTasks(pid, sortedKeys);
        }

        // Grant rewards. If this throws, transaction rolls back CLAIMED state.
        if (config != null) {
            grantRewards(playerId, config);
            log.info("[Task] Granted rewards for task '{}' → player {}: gold={}, exp={}",
                taskKey, playerId, config.goldReward(), config.expReward());
        }

        int newIndex = countClaimedTasks(pid, sortedKeys);
        log.info("[Task] Player {} advanced from task {} ('{}') to task {}",
            playerId, Math.max(newIndex - 1, 0), taskKey, newIndex);
        return newIndex;
    }

    /**
     * Claim all completed tasks for a player
     * Used by TaskHandler via TaskFeign
     */
    @Transactional
    public void claimAllCompletedTasks(String playerId) {
        log.info("Claiming all completed tasks for player: {}", playerId);

        List<TaskProgressEntity> completedTasks = taskProgressRepository
            .findByPlayerIdAndStatus(Long.parseLong(playerId), TaskStatus.COMPLETED);

        for (TaskProgressEntity task : completedTasks) {
            int updated = taskProgressRepository.markClaimedIfCompleted(task.getPlayerId(), task.getTaskKey(), Instant.now());
            if (updated <= 0) {
                continue;
            }

            // Grant rewards
            TaskDefinitionConfig config = findTaskConfig(task.getTaskKey());
            if (config != null) {
                grantRewards(playerId, config);
                log.info("Granted rewards for task {} - Gold: {}, Exp: {}",
                    task.getTaskKey(), config.goldReward(), config.expReward());
            }
        }

        log.info("Claimed {} tasks for player: {}", completedTasks.size(), playerId);
    }

    /**
     * Get task progress for specific task
     * Used by TaskHandler via TaskFeign
     */
    public Integer getTaskProgress(String playerId, String taskKey) {
        log.info("Getting task progress for player: {}, task: {}", playerId, taskKey);

        return taskProgressRepository
            .findByPlayerIdAndTaskKey(Long.parseLong(playerId), taskKey)
            .map(TaskProgressEntity::getProgressValue)
            .orElse(0);
    }

    /**
     * Get detailed task state so caller can distinguish IN_PROGRESS vs COMPLETED/CLAIMED.
     */
    public Map<String, Object> getTaskState(String playerId, String taskKey) {
        long pid = Long.parseLong(playerId);
        TaskDefinitionConfig config = findTaskConfig(taskKey);
        int target = config != null ? config.targetValue() : 0;

        TaskProgressEntity progress = taskProgressRepository
            .findByPlayerIdAndTaskKey(pid, taskKey)
            .orElse(null);

        TaskStatus status = progress != null ? progress.getStatus() : TaskStatus.NOT_STARTED;
        int current = progress != null ? progress.getProgressValue() : 0;

        boolean completed = status == TaskStatus.COMPLETED || status == TaskStatus.CLAIMED;
        boolean processing = status == TaskStatus.IN_PROGRESS || status == TaskStatus.NOT_STARTED;

        return Map.of(
            "taskKey", taskKey,
            "status", status.name(),
            "currentProgress", current,
            "targetValue", target,
            "completed", completed,
            "processing", processing,
            "claimed", status == TaskStatus.CLAIMED
        );
    }

    /**
     * Grant rewards to player via wallet-service and bag-service
     * OPTIMIZED: Parallel wallet and bag calls using Virtual Threads
     */
    private void grantRewards(String playerId, TaskDefinitionConfig config) {
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // 1. Grant gold and exp via wallet-service (async)
            if (config.goldReward() > 0 || config.expReward() > 0) {
                CompletableFuture<Void> walletFuture = CompletableFuture.runAsync(() -> {
                    List<WalletDTOs.Change> changes = new ArrayList<>();

                    if (config.goldReward() > 0) {
                        changes.add(WalletDTOs.Change.builder()
                            .itemId(1L) // itemId 1 = Gold
                            .amount((long) config.goldReward())
                            .build());
                    }

                    if (config.expReward() > 0) {
                        changes.add(WalletDTOs.Change.builder()
                            .itemId(2L) // itemId 2 = Experience
                            .amount((long) config.expReward())
                            .build());
                    }

                    WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                        .roleId(playerId)
                        .changes(changes)
                        .idemKey("task:" + playerId + ":" + config.key())
                        .reason(1001) // Task reward reason code
                        .build();

                    walletClient.addCurrency(playerId, walletReq);
                    log.info("Granted wallet rewards to player {}: gold={}, exp={}",
                        playerId, config.goldReward(), config.expReward());
                }, virtualExecutor);
                futures.add(walletFuture);
            }

            // 2. Grant items via bag-service (async)
            if (config.itemRewards() != null && !config.itemRewards().isEmpty()) {
                CompletableFuture<Void> bagFuture = CompletableFuture.runAsync(() -> {
                    List<BagDTOs.GrantItem> items = parseItemRewards(config.itemRewards());

                    if (!items.isEmpty()) {
                        String eventId = "task:" + playerId + ":" + config.key();
                        BagAddItemReq grantReq = BagAddItemReq.builder()
                            .userId(Long.parseLong(playerId))
                            .roleId(Long.parseLong(playerId))
                            .items(toBagAddItems(items))
                            .source("TASK_REWARD")
                            .idemKey(eventId)
                            .reason(1001)
                            .build();
                        bagClient.grantItems(grantReq);
                        log.info("Granted bag items to player {}: {} (parsedItems={}, eventId={})",
                                playerId, config.itemRewards(), items.size(), eventId);
                    }
                }, virtualExecutor);
                futures.add(bagFuture);
            }

            // Wait for all parallel operations to complete
            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

        } catch (Exception e) {
            log.error("Failed to grant rewards for task {} to player {}: {}",
                config.key(), playerId, e.getMessage(), e);
            throw new RuntimeException("Failed to grant task rewards: " + e.getMessage());
        }
    }

    /**
     * Parse item rewards from string format: "item:101:5,item:102:10"
     * Format: "item:<itemId>:<quantity>"
     */
    private List<BagDTOs.GrantItem> parseItemRewards(String itemRewards) {
        List<BagDTOs.GrantItem> items = new ArrayList<>();

        if (itemRewards == null || itemRewards.trim().isEmpty()) {
            return items;
        }

        String[] parts = itemRewards.split(",");
        Map<Integer, Integer> totals = new LinkedHashMap<>();
        for (String part : parts) {
            String[] tokens = part.trim().split(":");
            if (tokens.length >= 3 && "item".equals(tokens[0])) {
                try {
                    int itemId = Integer.parseInt(tokens[1]);
                    int quantity = Integer.parseInt(tokens[2]);
                    if (itemId > 0 && quantity > 0) {
                        totals.merge(itemId, quantity, Integer::sum);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid item reward format: {}", part);
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : totals.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            Integer bagType = resolveRewardBagType(itemId);
            Integer quality = resolveRewardQuality(itemId);

            BagDTOs.GrantItem.GrantItemBuilder builder = BagDTOs.GrantItem.builder()
                .itemId(itemId)
                .num(quantity)
                .bind(0);

            if (bagType != null) {
                builder.bagType(Math.max(0, bagType));
            }
            if (quality != null) {
                builder.quality(Math.max(1, quality));
            }

            items.add(builder.build());
        }

        return items;
    }

    private List<BagAddItemReq.Item> toBagAddItems(List<BagDTOs.GrantItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return items.stream()
                .map(item -> BagAddItemReq.Item.builder()
                        .itemId(item.getItemId())
                        .amount(item.getNum())
                        .quality(item.getQuality())
                        .bound(item.getBind() != null && item.getBind() != 0)
                        .bagType(item.getBagType())
                        .build())
                .toList();
    }

    private Integer resolveRewardBagType(int itemId) {
        Map<String, Object> meta = loadItemMeta(itemId);
        if (meta == null || meta.isEmpty()) {
            return null;
        }

        Integer direct = readInt(meta, "bagType", "bag_type");
        if (direct != null) {
            return direct;
        }

        String itemType = readText(meta, "itemType", "item_type", "type");
        if (itemType != null && "equip".equalsIgnoreCase(itemType.trim())) {
            return 1;
        }
        return null;
    }

    private Integer resolveRewardQuality(int itemId) {
        Map<String, Object> meta = loadItemMeta(itemId);
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        return readInt(meta, "quality", "color", "q");
    }

    @Cacheable(value = "taskItemMeta", key = "#itemId", unless = "#result == null || #result.isEmpty()")
    private Map<String, Object> loadItemMeta(int itemId) {
        if (itemMetaClient == null || itemId <= 0) {
            return Map.of();
        }
        try {
            Map<String, Object> meta = itemMetaClient.meta(itemId);
            return meta == null ? Map.of() : meta;
        } catch (Exception e) {
            log.warn("Cannot resolve item meta for task reward itemId={}: {}", itemId, e.getMessage());
            return Map.of();
        }
    }

    private Integer readInt(Map<String, Object> meta, String... keys) {
        if (meta == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = meta.get(key);
            if (value instanceof Number n) {
                return n.intValue();
            }
            if (value != null) {
                try {
                    return Integer.parseInt(String.valueOf(value).trim());
                } catch (Exception ignore) {
                    // ignore invalid format
                }
            }
        }
        return null;
    }

    private String readText(Map<String, Object> meta, String... keys) {
        if (meta == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = meta.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private Map<String, TaskDefinitionConfig> currentTaskConfigs() {
        if (taskDefinitionProvider == null) {
            return LOCAL_FALLBACK_TASK_CONFIGS;
        }
        Map<String, TaskDefinitionConfig> configs = taskDefinitionProvider.getTaskConfigs();
        if (configs == null || configs.isEmpty()) {
            return LOCAL_FALLBACK_TASK_CONFIGS;
        }
        return configs;
    }

    private TaskDefinitionConfig findTaskConfig(String taskKey) {
        return currentTaskConfigs().get(taskKey);
    }

    private String resolveReportTargetTaskKey(TaskReportReq req, Map<String, TaskDefinitionConfig> taskConfigs) {
        String reportedTaskKey = req.getTaskKey();
        if (reportedTaskKey == null || reportedTaskKey.isBlank()) {
            log.debug("[TaskResolve] player={} reportedKey=null/blank → ignored", req.getPlayerId());
            return null;
        }

        TaskDefinitionConfig directConfig = taskConfigs.get(reportedTaskKey);
        if (directConfig != null && directConfig.legacyConditionType() == null) {
            log.debug("[TaskResolve] player={} reportedKey={} → direct match (no legacyCondition)",
                req.getPlayerId(), reportedTaskKey);
            return reportedTaskKey;
        }

        long playerId = Long.parseLong(req.getPlayerId());
        List<String> sortedKeys = taskConfigs.keySet().stream()
            .sorted(taskKeyComparator())
            .toList();
        String currentTaskKey = findCurrentTaskKey(playerId, sortedKeys);
        if (currentTaskKey == null) {
            log.debug("[TaskResolve] player={} reportedKey={} → NO current task (all done or new player)",
                req.getPlayerId(), reportedTaskKey);
            return null;
        }

        if (currentTaskKey.equals(reportedTaskKey)) {
            log.debug("[TaskResolve] player={} reportedKey={} → exact match with current task",
                req.getPlayerId(), reportedTaskKey);
            return currentTaskKey;
        }

        TaskDefinitionConfig currentConfig = taskConfigs.get(currentTaskKey);
        Integer reportedConditionType = TaskConditionRegistry.resolveConditionType(reportedTaskKey);
        Integer currentConditionType = currentConfig != null ? currentConfig.legacyConditionType() : null;
        log.debug("[TaskResolve] player={} reportedKey={} reportedCondition={} | currentTask={} currentCondition={}",
            req.getPlayerId(), reportedTaskKey, reportedConditionType, currentTaskKey, currentConditionType);
        if (currentConditionType != null && currentConditionType.equals(reportedConditionType)) {
            log.debug("[TaskResolve] player={} reportedKey={} → matched via legacyCondition={} to currentTask={}",
                req.getPlayerId(), reportedTaskKey, reportedConditionType, currentTaskKey);
            return currentTaskKey;
        }
        log.debug("[TaskResolve] player={} reportedKey={} → NO MATCH (reportedCondition={} != currentCondition={}, currentTask={})",
            req.getPlayerId(), reportedTaskKey, reportedConditionType, currentConditionType, currentTaskKey);
        return null;
    }

    private int countClaimedTasks(long playerId, List<String> sortedKeys) {
        Map<String, TaskProgressEntity> progressMap = taskProgressRepository.findAllByPlayerId(playerId).stream()
            .collect(Collectors.toMap(TaskProgressEntity::getTaskKey, progress -> progress));

        int claimedCount = 0;
        for (String taskKey : sortedKeys) {
            TaskProgressEntity progress = progressMap.get(taskKey);
            if (progress == null || progress.getStatus() != TaskStatus.CLAIMED) {
                break;
            }
            claimedCount++;
        }
        return claimedCount;
    }

    private String findCurrentTaskKey(long playerId, List<String> sortedKeys) {
        Map<String, TaskProgressEntity> progressMap = taskProgressRepository.findAllByPlayerId(playerId).stream()
            .collect(Collectors.toMap(TaskProgressEntity::getTaskKey, progress -> progress));

        for (String taskKey : sortedKeys) {
            TaskProgressEntity progress = progressMap.get(taskKey);
            if (progress == null || progress.getStatus() != TaskStatus.CLAIMED) {
                return taskKey;
            }
        }
        return null;
    }

    private Comparator<String> taskKeyComparator() {
        return (left, right) -> {
            Long l = tryParseLong(left);
            Long r = tryParseLong(right);

            // Numeric task ids first, sorted by numeric value (2 before 10).
            if (l != null && r != null) {
                return Long.compare(l, r);
            }
            if (l != null) return -1;
            if (r != null) return 1;

            // Keep deterministic ordering for non-numeric legacy keys.
            return left.compareTo(right);
        };
    }

    private Long tryParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
