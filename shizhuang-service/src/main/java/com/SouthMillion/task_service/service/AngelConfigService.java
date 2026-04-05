package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.ShiZhuang.AngelConfigDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
// AngelConfigService disabled in shizhuang-service (belongs to angel-service)
// @Service
@RequiredArgsConstructor
public class AngelConfigService {

    private static final String ANGEL_PATH = "gameworld/logicconfig/angel.json";

    private final ConfigFeignClient configFeignClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;

    @Value("${shizhuang.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${shizhuang.config.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${shizhuang.config.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    // Cache config trong RAM (chỉ load 1 lần/lazy load)
    private AngelConfigDTO cachedConfig;

    /**
     * Tải và parse angel.json theo Redis-first strategy
     */
    public AngelConfigDTO getAngelConfig() {
        if (cachedConfig == null) {
            JsonNode jsonNode = loadConfigNode(false);
            try {
                cachedConfig = objectMapper.treeToValue(jsonNode, AngelConfigDTO.class);
            } catch (Exception e) {
                throw new RuntimeException("Parse angel.json thất bại!", e);
            }
        }
        return cachedConfig;
    }

    /**
     * Reload lại angel.json (nếu hot update)
     */
    public AngelConfigDTO reloadAngelConfig() {
        JsonNode jsonNode = loadConfigNode(true);
        try {
            cachedConfig = objectMapper.treeToValue(jsonNode, AngelConfigDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Parse angel.json thất bại!", e);
        }
        return cachedConfig;
    }

    private JsonNode loadConfigNode(boolean forceReload) {
        String redisKey = toRedisKey(ANGEL_PATH);

        if (redisEnabled && !forceReload) {
            try {
                String cached = redis.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    JsonNode node = objectMapper.readTree(cached);
                    touchRedisKey(redisKey);
                    log.debug("[AngelConfigService] Redis HIT path={}", ANGEL_PATH);
                    return node;
                }
                log.debug("[AngelConfigService] Redis MISS path={}", ANGEL_PATH);
            } catch (Exception e) {
                log.warn("[AngelConfigService] redis read failed path={} ex={}", ANGEL_PATH, e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        if (!forceReload && !allowRemoteFallbackOnMiss) {
            throw new IllegalStateException("angel.json missing from Redis while shizhuang.config.allow-remote-fallback-on-miss=false");
        }

        JsonNode remote = configFeignClient.getConfigFile(ANGEL_PATH);
        if (remote == null || remote.isNull()) {
            throw new RuntimeException("Config not found: " + ANGEL_PATH);
        }

        if (redisEnabled) {
            try {
                redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(remote), redisTtlHours, TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("[AngelConfigService] redis write failed path={} ex={}", ANGEL_PATH, e.toString());
            }
        }
        return remote;
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[AngelConfigService] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    // --- Ví dụ: Truy vấn các thông tin theo nhu cầu ---
    public Optional<AngelConfigDTO.AngelLevelCfg> getAngelLevelCfg(int level) {
        return getAngelConfig().getAngelCfg().stream()
                .filter(cfg -> cfg.getLevel() == level)
                .findFirst();
    }

    public Optional<AngelConfigDTO.AngelUpCfg> getAngelStageCfg(int stage) {
        return getAngelConfig().getAngelUp().stream()
                .filter(cfg -> cfg.getAngleStage() == stage)
                .findFirst();
    }

    // ... các hàm get theo id/seq khác tuỳ nghiệp vụ ...
}