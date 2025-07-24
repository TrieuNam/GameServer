package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.model.LimitCoreEntity;
import com.SouthMillion.user_service.repository.LimitCoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LimitCoreService {
    @Autowired
    private LimitCoreRepository repo;
    @Autowired
    private LimitCoreRedisService redisService;

    private static final int DEFAULT_CORE_LEVEL = 1;
    private static final int DEFAULT_CORE_COUNT = 5; // đọc từ config nếu muốn

    public List<Integer> getCoreLevels(Long userId) {
        List<Integer> coreLevels = redisService.getCoreLevels(userId);
        if (coreLevels != null) return coreLevels;

        List<LimitCoreEntity> entities = repo.findByIdUserId(userId);
        if (!entities.isEmpty()) {
            // Sắp xếp theo coreIndex
            entities.sort(Comparator.comparing(e -> e.getId().getCoreIndex()));
            coreLevels = entities.stream().map(LimitCoreEntity::getLevel).collect(Collectors.toList());
            redisService.setCoreLevels(userId, coreLevels);
            return coreLevels;
        }
        // Nếu user chưa có, trả list mặc định
        List<Integer> defaults = new ArrayList<>();
        for (int i = 0; i < DEFAULT_CORE_COUNT; i++) {
            defaults.add(DEFAULT_CORE_LEVEL);
        }
        return defaults;
    }

    public List<Integer> updateCoreLevel(Long userId, Integer type, Integer p1) {
        List<LimitCoreEntity> entities = repo.findByIdUserId(userId);
        if (entities.isEmpty()) {
            // Khởi tạo mặc định
            entities = new ArrayList<>();
            for (int i = 0; i < DEFAULT_CORE_COUNT; i++) {
                LimitCoreEntity e = new LimitCoreEntity();
                LimitCoreEntity.LimitCoreKey key = new LimitCoreEntity.LimitCoreKey();
                key.setUserId(userId);
                key.setCoreIndex(i);
                e.setId(key);
                e.setLevel(DEFAULT_CORE_LEVEL);
                entities.add(e);
            }
        }
        // Ví dụ: type=0, p1 là core index muốn tăng
        if (type == 0 && p1 != null && p1 >= 0 && p1 < entities.size()) {
            entities.get(p1).setLevel(entities.get(p1).getLevel() + 1);
        }
        // Lưu DB
        repo.saveAll(entities);
        // Update cache
        List<Integer> result = entities.stream().map(LimitCoreEntity::getLevel).collect(Collectors.toList());
        redisService.setCoreLevels(userId, result);
        return result;
    }
}