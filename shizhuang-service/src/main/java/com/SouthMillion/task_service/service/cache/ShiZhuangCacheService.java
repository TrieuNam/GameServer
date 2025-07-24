package com.SouthMillion.task_service.service.cache;

import com.SouthMillion.task_service.entity.ShiZhuangEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShiZhuangCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(String userId, int id) {
        return "shizhuang:" + userId + ":" + id;
    }

    public ShiZhuangEntity getFromCache(String userId, int id) {
        return (ShiZhuangEntity) redisTemplate.opsForValue().get(buildKey(userId, id));
    }

    public void putToCache(ShiZhuangEntity entity) {
        redisTemplate.opsForValue().set(buildKey(entity.getUserId(), entity.getId()), entity);
    }

    public void removeFromCache(String userId, int id) {
        redisTemplate.delete(buildKey(userId, id));
    }
}

