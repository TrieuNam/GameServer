package com.SouthMillion.box_service.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BoxCompareStateRepository {

    private static final String KEY_PREFIX = "box:compare:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${box.compare.ttl-minutes:15}")
    private long ttlMinutes;

    public void save(BoxDTOs.BoxCompareStateResp state) {
        if (state == null || state.getRoleId() == null) {
            return;
        }
        try {
            redis.opsForValue().set(key(state.getRoleId()), objectMapper.writeValueAsString(state), ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[box] save compare state failed roleId={} ex={}", state.getRoleId(), e.toString());
        }
    }

    public Optional<BoxDTOs.BoxCompareStateResp> find(Long roleId) {
        if (roleId == null) {
            return Optional.empty();
        }
        try {
            String raw = redis.opsForValue().get(key(roleId));
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(raw, BoxDTOs.BoxCompareStateResp.class));
        } catch (Exception e) {
            log.warn("[box] load compare state failed roleId={} ex={}", roleId, e.toString());
            return Optional.empty();
        }
    }

    public void delete(Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            redis.delete(key(roleId));
        } catch (Exception e) {
            log.warn("[box] delete compare state failed roleId={} ex={}", roleId, e.toString());
        }
    }

    private String key(Long roleId) {
        return KEY_PREFIX + roleId;
    }
}