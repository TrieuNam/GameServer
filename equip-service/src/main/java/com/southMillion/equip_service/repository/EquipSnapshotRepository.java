package com.SouthMillion.equip_service.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EquipSnapshotRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${equip.snapshot.redis-prefix:equip:snapshot:}")
    private String keyPrefix;

    @Value("${equip.snapshot.ttl-seconds:3600}")
    private long ttlSeconds;

    public void save(Long roleId, int equipType, EquipDTOs.EquipItem snapshot) {
        if (roleId == null || equipType < 0 || snapshot == null || snapshot.getItemId() <= 0) {
            return;
        }
        String key = key(roleId, equipType);
        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            if (ttlSeconds > 0) {
                redis.opsForValue().set(key, payload, ttlSeconds, TimeUnit.SECONDS);
            } else {
                redis.opsForValue().set(key, payload);
            }
        } catch (Exception e) {
            log.debug("[equip] snapshot save failed roleId={} equipType={} ex={}", roleId, equipType, e.toString());
        }
    }

    public Optional<EquipDTOs.EquipItem> find(Long roleId, int equipType) {
        if (roleId == null || equipType < 0) {
            return Optional.empty();
        }
        String key = key(roleId, equipType);
        try {
            String payload = redis.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            EquipDTOs.EquipItem snapshot = objectMapper.readValue(payload, EquipDTOs.EquipItem.class);
            if (snapshot == null || snapshot.getItemId() <= 0) {
                redis.delete(key);
                return Optional.empty();
            }
            touch(key);
            return Optional.of(snapshot);
        } catch (Exception e) {
            log.debug("[equip] snapshot load failed roleId={} equipType={} ex={}", roleId, equipType, e.toString());
            try {
                redis.delete(key);
            } catch (Exception ignored) {
                // ignore corrupt-cache cleanup failure
            }
            return Optional.empty();
        }
    }

    public void delete(Long roleId, int equipType) {
        if (roleId == null || equipType < 0) {
            return;
        }
        try {
            redis.delete(key(roleId, equipType));
        } catch (Exception e) {
            log.debug("[equip] snapshot delete failed roleId={} equipType={} ex={}", roleId, equipType, e.toString());
        }
    }

    public int deleteByRole(Long roleId) {
        if (roleId == null) {
            return 0;
        }
        String pattern = keyPrefix + roleId + ":*";
        try {
            var keys = redis.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            Long deleted = redis.delete(keys);
            return deleted == null ? 0 : deleted.intValue();
        } catch (Exception e) {
            log.debug("[equip] snapshot delete-by-role failed roleId={} ex={}", roleId, e.toString());
            return 0;
        }
    }

    private void touch(String key) {
        if (key == null || key.isBlank() || ttlSeconds <= 0) {
            return;
        }
        try {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("[equip] snapshot touch ttl failed key={} ex={}", key, e.toString());
        }
    }

    private String key(Long roleId, int equipType) {
        return keyPrefix + roleId + ":" + equipType;
    }
}
