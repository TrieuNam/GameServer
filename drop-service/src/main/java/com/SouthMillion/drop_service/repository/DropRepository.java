package com.SouthMillion.drop_service.repository;

import com.SouthMillion.drop_service.config.AppProperties;
import com.SouthMillion.drop_service.service.client.ConfigFeign;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.SouthMillion.dto.drop.DropXml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DropRepository {
    private final ConfigFeign cfg;
    private final AppProperties props;
    private final Cache<Integer, CompiledDrop> compiled; // inject từ DropBeans
    private final XmlMapper xml;                         // inject từ DropBeans
    private final StringRedisTemplate redis;

    @Value("${drop.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${drop.config.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${drop.config.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    private volatile Set<Integer> knownDropIds = Set.of();
    private final Map<Integer, String> etags = new ConcurrentHashMap<>();

    // ===== Public APIs
    public Set<Integer> listDropIds() {
        ensureListLoaded();
        return knownDropIds;
    }

    public CompiledDrop getCompiled(int dropId) {
        var cached = compiled.getIfPresent(dropId);
        if (cached != null) return cached;

        String path = dropConfigPath(dropId);
        String redisKey = toRedisKey(path);

        if (redisEnabled) {
            try {
                String cachedXml = redis.opsForValue().get(redisKey);
                if (StringUtils.hasText(cachedXml)) {
                    CompiledDrop cd = parse(cachedXml.getBytes(StandardCharsets.UTF_8));
                    compiled.put(dropId, cd);
                    touchRedisKey(redisKey);
                    log.debug("[DropRepository] Redis HIT path={}", path);
                    return cd;
                }
                log.debug("[DropRepository] Redis MISS path={}", path);
            } catch (Exception e) {
                log.warn("[DropRepository] redis read failed path={} ex={}", path, e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        if (!allowRemoteFallbackOnMiss) {
            throw new IllegalStateException("Drop XML missing from Redis while drop.config.allow-remote-fallback-on-miss=false: " + path);
        }

        String etag = etags.get(dropId);
        ResponseEntity<byte[]> resp = cfg.getFile(path, etag);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            CompiledDrop cd = parse(resp.getBody());
            compiled.put(dropId, cd);
            var newTag = resp.getHeaders().getETag();
            if (StringUtils.hasText(newTag)) etags.put(dropId, newTag);
            cacheRawXml(redisKey, resp.getBody());
            return cd;
        }
        if (resp.getStatusCode().value() == 304) {
            var existed = compiled.getIfPresent(dropId);
            if (existed != null) return existed;
            // hiếm khi vừa 304 vừa chưa có cache (race) -> fetch lại không gắn If-None-Match
            resp = cfg.getFile(path, null);
            CompiledDrop cd = parse(resp.getBody());
            compiled.put(dropId, cd);
            var newTag = resp.getHeaders().getETag();
            if (StringUtils.hasText(newTag)) etags.put(dropId, newTag);
            cacheRawXml(redisKey, resp.getBody());
            return cd;
        }
        throw new IllegalArgumentException("Cannot load drop " + dropId + " HTTP=" + resp.getStatusCode());
    }

    // ===== Helpers
    private CompiledDrop parse(byte[] xmlBytes) {
        try {
            DropXml x = xml.readValue(xmlBytes, DropXml.class);
            return new CompiledDrop(x);
        } catch (Exception e) {
            throw new RuntimeException("Parse drop xml failed: " + e.getMessage(), e);
        }
    }

    private void cacheRawXml(String redisKey, byte[] xmlBytes) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || xmlBytes == null) {
            return;
        }
        try {
            redis.opsForValue().set(redisKey, new String(xmlBytes, StandardCharsets.UTF_8), redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[DropRepository] redis write failed key={} ex={}", redisKey, e.toString());
        }
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[DropRepository] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    private void ensureListLoaded() {
        if (knownDropIds.isEmpty()) reloadList();
    }

    @Scheduled(fixedDelayString = "#{${app.config.managerReloadSeconds:0} > 0 ? ${app.config.managerReloadSeconds:0} * 1000 : 2147483647}")
    public void periodicReload() {
        if (props.getConfig().getManagerReloadSeconds() <= 0) return;
        reloadList();
    }

    private synchronized void reloadList() {
        try {
            Set<Integer> ids = new HashSet<>(props.getConfig().resolveKnownDropIds());
            knownDropIds = Set.copyOf(ids);
            // evict cache của id không còn trong list
            compiled.asMap().keySet().removeIf(id -> !knownDropIds.contains(id));
            etags.keySet().removeIf(id -> !knownDropIds.contains(id));
            log.info("drop id list loaded from properties: {} tables", knownDropIds.size());
        } catch (Exception e) {
            log.warn("reload drop list failed: {}", e.toString());
        }
    }

    private String dropConfigPath(int dropId) {
        String pattern = props.getConfig().getDropPathTemplate();
        if (!StringUtils.hasText(pattern)) {
            pattern = "gameworld/drop/%s.xml";
        }
        return pattern.formatted(dropId);
    }
}