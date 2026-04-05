package com.SouthMillion.drop_service.config;

import com.SouthMillion.drop_service.service.client.ConfigFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Warms known drop XML configs into Redis at startup so runtime can stay strict Redis-first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DropConfigRedisPreloader {

    private final AppProperties props;
    private final ConfigFeign cfg;
    private final StringRedisTemplate redis;
    private final DropRedisStatusService statusService;

    @Value("${drop.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${drop.config.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${drop.config.preload-on-startup:true}")
    private boolean preloadOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void preloadKnownDrops() {
        if (!redisEnabled || !preloadOnStartup) {
            log.info("[drop-preload] skipped (redisEnabled={}, preloadOnStartup={})", redisEnabled, preloadOnStartup);
            return;
        }
        rewarmDrops(new LinkedHashSet<>(props.getConfig().resolveKnownDropIds()), false, "startup");
    }

    public RewarmResult rewarmMissingDrops() {
        List<Integer> knownIds = props.getConfig().resolveKnownDropIds();
        List<Integer> missing = statusService.findMissingDropIds(knownIds);
        return rewarmDrops(new LinkedHashSet<>(missing), true, "manual-missing");
    }

    public RewarmResult rewarmTargetedDrops(List<Integer> dropIds, boolean forceRefresh) {
        return rewarmDrops(new LinkedHashSet<>(dropIds == null ? List.of() : dropIds), forceRefresh, "manual-targeted");
    }

    private RewarmResult rewarmDrops(Set<Integer> ids, boolean forceRefresh, String source) {
        if (!redisEnabled) {
            return finish(source, forceRefresh, ids == null ? Set.of() : ids, 0, 0, 0, List.of(), "redis disabled");
        }

        if (ids == null || ids.isEmpty()) {
            return finish(source, forceRefresh, Set.of(), 0, 0, 0, List.of(), "no drop ids to warm");
        }

        int warmed = 0;
        int hits = 0;
        int failed = 0;
        List<Integer> failedIds = new ArrayList<>();

        for (Integer dropId : ids) {
            String path = dropConfigPath(dropId);
            String redisKey = toRedisKey(path);
            try {
                if (!forceRefresh) {
                    String cached = redis.opsForValue().get(redisKey);
                    if (StringUtils.hasText(cached)) {
                        redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
                        hits++;
                        continue;
                    }
                }

                ResponseEntity<byte[]> resp = cfg.getFile(path, null);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    redis.opsForValue().set(redisKey, new String(resp.getBody(), StandardCharsets.UTF_8), redisTtlHours, TimeUnit.HOURS);
                    warmed++;
                } else {
                    failed++;
                    failedIds.add(dropId);
                    log.warn("[drop-preload] failed path={} status={}", path, resp.getStatusCode());
                }
            } catch (Exception e) {
                failed++;
                failedIds.add(dropId);
                log.warn("[drop-preload] unexpected failure path={} ex={}", path, e.toString());
            }
        }

        return finish(source, forceRefresh, ids, warmed, hits, failed, failedIds, "completed");
    }

    private RewarmResult finish(String source, boolean forceRefresh, Set<Integer> ids,
                                int warmed, int hits, int failed, List<Integer> failedIds, String reason) {
        log.info("[drop-preload] done source={} forceRefresh={} hits={} warmed={} failed={} total={}",
                source, forceRefresh, hits, warmed, failed, ids.size());

        DropRedisStatusService.DropRedisStatus status = statusService.snapshot();
        if (status.ready()) {
            log.info("[drop-preload] redis-ready cachedCount={} knownDropCount={}",
                    status.cachedCount(), status.knownDropCount());
        } else {
            log.warn("[drop-preload] redis-missing missingCount={} firstMissingId={} missingRanges={}",
                    status.missingCount(), status.firstMissingId(), status.missingRanges());
        }

        return new RewarmResult(
                source,
                forceRefresh,
                ids.size(),
                warmed,
                hits,
                failed,
                failedIds,
                status.ready(),
                status.missingCount(),
                status.missingRanges(),
                reason
        );
    }

    private String dropConfigPath(int dropId) {
        String pattern = props.getConfig().getDropPathTemplate();
        if (!StringUtils.hasText(pattern)) {
            pattern = "gameworld/drop/%s.xml";
        }
        return pattern.formatted(dropId);
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    public record RewarmResult(
            String source,
            boolean forceRefresh,
            int requestedCount,
            int warmedCount,
            int alreadyCachedCount,
            int failedCount,
            List<Integer> failedIds,
            boolean ready,
            int missingCountAfter,
            List<String> missingRangesAfter,
            String reason
    ) {}
}
