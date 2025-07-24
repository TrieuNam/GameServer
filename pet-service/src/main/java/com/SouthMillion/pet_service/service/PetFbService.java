package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.entity.PetFbEntity;
import com.SouthMillion.pet_service.repository.PetFbRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.globalserver.PetFbDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PetFbService {
    @Autowired
    private PetFbRepository repo;
    @Autowired
    private StringRedisTemplate redis;
    private static final String PETFB_KEY = "petfb:%d";

    public PetFbDTO getInfo(Long userId) {
        String key = String.format(PETFB_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        PetFbEntity e = repo.findById(userId).orElseGet(() -> {
            PetFbEntity entity = new PetFbEntity();
            entity.setUserId(userId);
            entity.setPassLevel(0);
            entity.setFetchFlag(0L);
            repo.save(entity);
            return entity;
        });
        PetFbDTO dto = toDto(e);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    // Đánh phó bản/thưởng
    public void operate(Long userId, Integer type, Integer level) {
        PetFbEntity entity = repo.findById(userId).orElseThrow();
        // type = 1: Đánh/challenge
        if (type == 1) {
            // Nếu qua level mới -> update
            if (entity.getPassLevel() < level)
                entity.setPassLevel(level);
            // Đặt fetchFlag (bitmask) cho thưởng mỗi level nếu cần
            entity.setFetchFlag(entity.getFetchFlag() | (1L << level));
        }
        repo.save(entity);
        redis.delete(String.format(PETFB_KEY, userId));
    }

    private PetFbDTO toDto(PetFbEntity e) {
        PetFbDTO dto = new PetFbDTO();
        dto.setPassLevel(e.getPassLevel());
        dto.setFetchFlag(e.getFetchFlag());
        return dto;
    }

    private String toJson(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private PetFbDTO parse(String json) {
        try {
            return new ObjectMapper().readValue(json, PetFbDTO.class);
        } catch (Exception ex) {
            return new PetFbDTO();
        }
    }
}
