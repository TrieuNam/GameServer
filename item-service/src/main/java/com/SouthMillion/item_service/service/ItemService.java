package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.ItemEntity;
import com.SouthMillion.item_service.repository.ItemRepository;
import com.SouthMillion.item_service.service.config.ItemConfigService;
import org.SouthMillion.dto.item.Knapsack.ItemConfigDTO;
import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository repo;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ItemConfigService itemConfigService;
    private static final String REDIS_KEY_FMT = "item:user:%d";

    public List<ItemDTO> getAllByUser(String userId) {
        String key = String.format(REDIS_KEY_FMT, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) {
            return JsonUtil.fromJsonList(cache, ItemDTO.class);
        }
        List<ItemDTO> list = repo.findByUserId(userId).stream()
                .map(ItemEntity::fromEntity)
                .collect(Collectors.toList());
        redis.opsForValue().set(key, JsonUtil.toJson(list), Duration.ofMinutes(5));
        return list;
    }

    public ItemDTO getSingle(String userId, Integer itemId) {
        return repo.findByUserIdAndItemId(userId, itemId)
                .map(ItemEntity::fromEntity).orElse(null);
    }

    public Integer getUserItemCount(String userId, Integer itemId) {
        Optional<ItemEntity> item = repo.findByUserIdAndItemId(userId, itemId);
        return item != null ? item.get().getCount() : 0;
    }

    public void addItem(String userId, Integer itemId, int count) {
        Object config = itemConfigService.getConfigById(itemId);
        if (config == null) throw new IllegalArgumentException("Invalid itemId " + itemId);

        ItemEntity entity = repo.findByUserIdAndItemId(userId, itemId)
                .orElseGet(() -> {
                    ItemEntity e = new ItemEntity();
                    e.setUserId(userId);
                    e.setItemId(itemId);
                    e.setCount(0);
                    return e;
                });
        entity.setCount(entity.getCount() + count);
        repo.save(entity);
        invalidateCache(userId);
    }

    public boolean consumeItem(String userId, Integer itemId, int count) {
        Object config = itemConfigService.getConfigById(itemId);
        if (config == null) return false;

        Optional<ItemEntity> opt = repo.findByUserIdAndItemId(userId, itemId);
        if (opt.isPresent() && opt.get().getCount() >= count) {
            ItemEntity e = opt.get();
            e.setCount(e.getCount() - count);
            repo.save(e);
            invalidateCache(userId);
            return true;
        }
        return false;
    }

    private void invalidateCache(String userId) {
        redis.delete(String.format(REDIS_KEY_FMT, userId));
    }

    public boolean hasEnoughItem(String userId, int itemId, int requiredCount) {
        // Kiểm tra itemId hợp lệ
        Object config = itemConfigService.getConfigById(itemId);
        if (config == null) return false;

        Optional<ItemEntity> entityOpt = repo.findByUserIdAndItemId(userId, itemId);
        return entityOpt.isPresent() && entityOpt.get().getCount() >= requiredCount;
    }

}
