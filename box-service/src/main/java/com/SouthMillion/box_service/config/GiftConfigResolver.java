package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiftConfigResolver {

    private static final String GIFT_PATH = "gameworld/item/gift.json";

    private final ConfigFeign cfg;
    private final BoxProperties props;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${box.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${box.config.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${box.config.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    private volatile Integer cachedId;

    public int resolveStarterGiftItemId() {

        // 1) Ưu tiên cấu hình cố định
        if (props.getInitItemId() > 0) {
            return Math.toIntExact(props.getInitItemId());
        }
        if (cachedId != null) return cachedId;

        Map<String, Object> root = loadGiftRoot();

        @SuppressWarnings("unchecked")
        var defGift = (java.util.List<java.util.Map<String, Object>>) root.get("DefGift");
        if (defGift == null || defGift.isEmpty()) {
            throw new IllegalStateException("gift.json thiếu DefGift");
        }

        String want = props.getStarterGiftName();
        if (want == null || want.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình box.starter-gift-name hoặc box.init-item-id");
        }

        Map<String, Object> match = defGift.stream()
                .filter(it -> want.equals(String.valueOf(it.get("name"))))
                .findFirst()
                .orElseGet(() -> defGift.stream()
                        .filter(it -> String.valueOf(it.get("name")).contains(want))
                        .findFirst()
                        .orElse(null));

        if (match == null) {
            throw new IllegalStateException("Không tìm thấy gift với tên: " + want);
        }

        Object idVal = match.get("id");
        int id;
        try {
            // id có thể là số hoặc chuỗi
            id = (idVal instanceof Number n) ? n.intValue() : Integer.parseInt(String.valueOf(idVal));
        } catch (Exception e) {
            throw new IllegalStateException("gift.id không hợp lệ: " + idVal);
        }
        cachedId = id;
        return id;
    }

    private Map<String, Object> loadGiftRoot() {
        String redisKey = toRedisKey(GIFT_PATH);

        if (redisEnabled) {
            try {
                String cached = redis.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    Map<String, Object> root = om.readValue(cached, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    touchRedisKey(redisKey);
                    log.debug("[GiftConfigResolver] Redis HIT path={}", GIFT_PATH);
                    return root;
                }
                log.debug("[GiftConfigResolver] Redis MISS path={}", GIFT_PATH);
            } catch (Exception e) {
                log.warn("[GiftConfigResolver] redis read failed path={} ex={}", GIFT_PATH, e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        if (!allowRemoteFallbackOnMiss) {
            throw new IllegalStateException("gift.json missing from Redis while box.config.allow-remote-fallback-on-miss=false");
        }

        var resp = cfg.getFile(GIFT_PATH, null);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Không lấy được gift.json, status=" + resp.getStatusCode());
        }

        try {
            String body = new String(resp.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> root = om.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            if (redisEnabled) {
                redis.opsForValue().set(redisKey, body, redisTtlHours, TimeUnit.HOURS);
            }
            return root;
        } catch (Exception e) {
            throw new IllegalStateException("Parse gift.json thất bại: " + e.getMessage(), e);
        }
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[GiftConfigResolver] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}