package com.SouthMillion.gameworld_service.service;

import com.SouthMillion.gameworld_service.entity.MainFbEntity;
import com.SouthMillion.gameworld_service.repository.MainFbRepository;
import com.SouthMillion.gameworld_service.service.cache.MainFbCache;
import org.SouthMillion.dto.gameworld.MainFbDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static com.SouthMillion.gameworld_service.entity.MainFbEntity.fromEntity;

@Service
public class MainFbService {
    @Autowired
    private MainFbRepository repo;
    @Autowired
    private MainFbCache cache; // có thể bỏ nếu không dùng Redis

    public MainFbDTO getOrInit(Long userId) {
        MainFbDTO cached = cache.get(userId);
        if (cached != null) return cached;

        MainFbEntity entity = repo.findById(userId).orElseGet(() -> {
            MainFbEntity e = new MainFbEntity();
            e.setUserId(userId);
            repo.save(e);
            return e;
        });
        MainFbDTO dto = fromEntity(entity);
        cache.save(dto); // cache vào Redis
        return dto;
    }

    public MainFbDTO update(Long userId, Consumer<MainFbEntity> updater) {
        MainFbEntity entity = repo.findById(userId).orElseThrow();
        updater.accept(entity);
        repo.save(entity);
        MainFbDTO dto = fromEntity(entity);
        cache.save(dto);
        return dto;
    }
}