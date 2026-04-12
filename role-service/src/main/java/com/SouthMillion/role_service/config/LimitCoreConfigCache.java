package com.SouthMillion.role_service.config;

import com.SouthMillion.role_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads and caches limit_core_auto.json from config-service.
 *
 * <p>Provides typed views:
 * <ul>
 *   <li>{@link CoreEntry}    – one row from {@code core[]} (levelUp config)</li>
 *   <li>{@link CoreboxEntry} – one row from {@code corebox[]} (draw config)</li>
 *   <li>{@link #getPrice1()}, {@link #getPrice2()}, {@link #getRewardNum()} – other[] fields</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimitCoreConfigCache {

    private static final int NUM_CORE_TYPES = 6;

    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${limit-core.config.path:gameworld/other/limit_core_auto.json}")
    private String configPath;

    // ── Parsed data ────────────────────────────────────────────────────────
    /** limit_tpye → sorted list of CoreEntry by limit_level */
    private final AtomicReference<Map<Integer, List<CoreEntry>>>    coreByType    = new AtomicReference<>(Map.of());
    /** box_type → list of CoreboxEntry */
    private final AtomicReference<Map<Integer, List<CoreboxEntry>>> coreboxByType = new AtomicReference<>(Map.of());

    @Getter private volatile int price1     = 100;
    @Getter private volatile int price2     = 250;
    @Getter private volatile int rewardNum  = 3;

    // ── ETag ───────────────────────────────────────────────────────────────
    private final AtomicReference<String> etag = new AtomicReference<>(null);

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        refresh();
    }

    @Scheduled(initialDelay = 120_000, fixedDelayString = "${limit-core.config.refresh-interval-ms:120000}")
    public void refresh() {
        try {
            ResponseEntity<byte[]> res = configFeign.getFile(configPath, etag.get());
            if (res.getStatusCode() == HttpStatus.NOT_MODIFIED) return;
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.warn("[LimitCoreConfigCache] Unexpected status {} for path={}", res.getStatusCode(), configPath);
                return;
            }
            String json = new String(res.getBody(), StandardCharsets.UTF_8);
            parse(json);
            // update etag
            List<String> etags = res.getHeaders().get("ETag");
            if (etags != null && !etags.isEmpty()) etag.set(etags.get(0));
            log.info("[LimitCoreConfigCache] Loaded OK — coreTypes={} boxTypes={}",
                    coreByType.get().size(), coreboxByType.get().size());
        } catch (Exception e) {
            log.warn("[LimitCoreConfigCache] Failed to load config path={}: {}", configPath, e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the CoreEntry for a given limitType at a given level.
     * Returns empty if already at max or type not in config.
     */
    public Optional<CoreEntry> getCoreEntry(int limitType, int level) {
        List<CoreEntry> entries = coreByType.get().get(limitType);
        if (entries == null) return Optional.empty();
        return entries.stream()
                .filter(e -> e.limitLevel() == level)
                .findFirst();
    }

    /**
     * Returns all corebox entries for the given boxType (0=free, 1=price1, 2=price2).
     */
    public List<CoreboxEntry> getCoreboxEntries(int boxType) {
        return coreboxByType.get().getOrDefault(boxType, Collections.emptyList());
    }

    public boolean isLoaded() {
        return !coreByType.get().isEmpty();
    }

    // ── Parsing ───────────────────────────────────────────────────────────

    private void parse(String json) throws Exception {
        JsonNode root = om.readTree(json);

        // Parse core[]
        Map<Integer, List<CoreEntry>> byType = new HashMap<>();
        JsonNode coreArr = root.path("core");
        for (JsonNode n : coreArr) {
            int limitType  = n.path("limit_tpye").asInt(0);  // note the typo in config
            int limitLevel = n.path("limit_level").asInt(0);
            int parm       = n.path("parm").asInt(0);
            int needItemId = n.path("need_item_id").asInt(0);
            int needNum    = n.path("need_core_num").asInt(0);
            byType.computeIfAbsent(limitType, k -> new ArrayList<>())
                  .add(new CoreEntry(limitType, limitLevel, parm, needItemId, needNum));
        }
        // Sort by level
        byType.values().forEach(list -> list.sort(Comparator.comparingInt(CoreEntry::limitLevel)));
        coreByType.set(Collections.unmodifiableMap(byType));

        // Parse corebox[]
        Map<Integer, List<CoreboxEntry>> boxMap = new HashMap<>();
        JsonNode boxArr = root.path("corebox");
        for (JsonNode n : boxArr) {
            int boxType    = n.path("box_type").asInt(0);
            int boxItem    = n.path("box_item").asInt(0);
            int boxItemMin = n.path("box_item_min").asInt(1);
            int boxItemMax = n.path("box_item_max").asInt(1);
            int boxRate    = n.path("box_rate").asInt(1);
            boxMap.computeIfAbsent(boxType, k -> new ArrayList<>())
                  .add(new CoreboxEntry(boxType, boxItem, boxItemMin, boxItemMax, boxRate));
        }
        coreboxByType.set(Collections.unmodifiableMap(boxMap));

        // Parse other[0]
        JsonNode otherArr = root.path("other");
        if (otherArr.isArray() && otherArr.size() > 0) {
            JsonNode other = otherArr.get(0);
            price1    = other.path("price1").asInt(100);
            price2    = other.path("price2").asInt(250);
            rewardNum = other.path("reward_num").asInt(3);
        }
    }

    // ── Records ───────────────────────────────────────────────────────────

    public record CoreEntry(int limitType, int limitLevel, int parm, int needItemId, int needCoreNum) {}

    public record CoreboxEntry(int boxType, int boxItem, int boxItemMin, int boxItemMax, int boxRate) {}
}
