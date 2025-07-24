package com.SouthMillion.item_service.service.cache;

import com.SouthMillion.item_service.entity.ItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(String userId, int itemId) {
        return "item:" + userId + ":" + itemId;
    }

    public ItemEntity getItemFromCache(String userId, int itemId) {
        return (ItemEntity) redisTemplate.opsForValue().get(buildKey(userId, itemId));
    }

    public void putItemToCache(ItemEntity item) {
        redisTemplate.opsForValue().set(buildKey(item.getUserId(), item.getItemId()), item);
    }

    public void removeItemFromCache(String userId, int itemId) {
        redisTemplate.delete(buildKey(userId, itemId));
    }
}