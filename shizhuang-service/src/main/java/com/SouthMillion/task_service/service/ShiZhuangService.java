package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.entity.ShiZhuangEntity;
import com.SouthMillion.task_service.repository.ShiZhuangRepository;
import com.SouthMillion.task_service.service.cache.ShiZhuangCacheService;
import com.SouthMillion.task_service.service.producer.ShiZhuangEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiZhuangService {
    private final ShiZhuangRepository repo;
    private final ShiZhuangCacheService cache;
    private final ShiZhuangEventProducer eventProducer;

    public ShiZhuangDto get(String userId, int id) {
        ShiZhuangEntity cached = cache.getFromCache(userId, id);
        if (cached != null) return toDto(cached);

        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(userId, id);
        if (entity.isPresent()) {
            cache.putToCache(entity.get());
            return toDto(entity.get());
        }
        return null;
    }

    public List<ShiZhuangDto> getAll(String userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public ShiZhuangDto addOrUpdate(ShiZhuangDto dto) {
        ShiZhuangEntity entity = repo.findByUserIdAndId(dto.getUserId(), dto.getId() != 0 ? dto.getId() : -1)
                .orElse(ShiZhuangEntity.builder().userId(dto.getUserId()).level(dto.getLevel()).build());
        entity.setLevel(dto.getLevel());
        ShiZhuangEntity saved = repo.save(entity);
        cache.putToCache(saved);

        // Produce Kafka event khi thêm hoặc update
        eventProducer.sendEvent(
                saved.getUserId(),
                "SHIZHUANG_CHANGED",
                toDto(saved)
        );
        return toDto(saved);
    }

    public boolean delete(String userId, int id) {
        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(userId, id);
        if (entity.isPresent()) {
            repo.delete(entity.get());
            cache.removeFromCache(userId, id);

            // Produce Kafka event khi xóa
            eventProducer.sendEvent(
                    userId,
                    "SHIZHUANG_DELETED",
                    ShiZhuangDto.builder()
                            .id(id)
                            .userId(userId)
                            .level(entity.get().getLevel())
                            .build()
            );
            return true;
        }
        return false;
    }

    public ShiZhuangDto toDto(ShiZhuangEntity entity) {
        return ShiZhuangDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .level(entity.getLevel())
                .build();
    }
}
