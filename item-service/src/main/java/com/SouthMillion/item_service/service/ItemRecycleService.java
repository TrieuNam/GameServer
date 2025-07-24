package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.ItemRecycleEntity;
import com.SouthMillion.item_service.repository.ItemRecycleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.ItemRecycleDTO;
import org.SouthMillion.dto.item.ItemRecycleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ItemRecycleService {
    @Autowired
    private ItemRecycleRepository repo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private KafkaTemplate<String, ItemRecycleEvent> kafka;

    private static final String ITEMRECYCLE_KEY = "itemrecycle:%d";

    public ItemRecycleDTO getInfo(Long userId) {
        String key = String.format(ITEMRECYCLE_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        ItemRecycleEntity entity = repo.findById(userId).orElseGet(() -> {
            ItemRecycleEntity e = new ItemRecycleEntity();
            e.setUserId(userId);
            e.setLevel(1);
            e.setExp(0L);
            repo.save(e);
            return e;
        });
        ItemRecycleDTO dto = toDto(entity);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    public void levelUp(Long userId, List<Integer> itemIds) {
        ItemRecycleEntity entity = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User chưa có dữ liệu tái chế"));
        long addExp = itemIds.size() * 100L;
        long totalExp = entity.getExp() + addExp;

        int upLevel = 0;
        while (totalExp >= 1000) {
            upLevel++;
            totalExp -= 1000;
        }
        entity.setLevel(entity.getLevel() + upLevel);
        entity.setExp(totalExp);

        repo.save(entity);
        ItemRecycleDTO dto = toDto(entity);
        redis.opsForValue().set(String.format(ITEMRECYCLE_KEY, userId), toJson(dto), Duration.ofMinutes(10));
        kafka.send("itemrecycle-event", new ItemRecycleEvent(userId, itemIds));
    }

    private ItemRecycleDTO toDto(ItemRecycleEntity entity) {
        ItemRecycleDTO dto = new ItemRecycleDTO();
        dto.setLevel(entity.getLevel());
        dto.setExp(entity.getExp());
        return dto;
    }
    private ItemRecycleDTO parse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, ItemRecycleDTO.class);
        } catch (Exception e) {
            return new ItemRecycleDTO();
        }
    }
    private String toJson(Object o) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}