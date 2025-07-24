package com.southMillion.equip_service.service.cache;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EquipCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(String userId, int equipType) {
        return "equip:" + userId + ":" + equipType;
    }

    public EquipDto getEquipFromCache(String userId, int equipType) {
        return (EquipDto) redisTemplate.opsForValue().get(buildKey(userId, equipType));
    }

    public void putEquipToCache(EquipDto equip, long cacheMinutes) {
        redisTemplate.opsForValue().set(buildKey(equip.getUserId(), equip.getEquipType()), equip, cacheMinutes, TimeUnit.MINUTES);
    }

    public void removeEquipFromCache(String userId, int equipType) {
        redisTemplate.delete(buildKey(userId, equipType));
    }
}