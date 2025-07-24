package com.southMillion.equip_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.southMillion.equip_service.entity.LimitCoreEntity;
import com.southMillion.equip_service.repository.LimitCoreRepository;
import org.SouthMillion.dto.item.LimitCoreDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
public class LimitCoreService {
    @Autowired
    private LimitCoreRepository repo;
    @Autowired
    private StringRedisTemplate redis;
    private static final String LIMITCORE_KEY = "limitcore:%d";

    public LimitCoreDTO getInfo(Long userId) {
        String key = String.format(LIMITCORE_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        LimitCoreEntity e = repo.findById(userId).orElseGet(() -> {
            LimitCoreEntity entity = new LimitCoreEntity();
            entity.setUserId(userId);
            entity.setCoreLevelJson("[0,0,0]");
            repo.save(entity);
            return entity;
        });
        LimitCoreDTO dto = toDto(e);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    // opType = 0: nâng cấp, 1: quay/lucky
    public void operate(Long userId, Integer opType, Integer p1) {
        LimitCoreEntity entity = repo.findById(userId).orElseThrow();
        List<Integer> core = parseList(entity.getCoreLevelJson());

        if (opType == 0 && p1 < core.size()) {
            // Nâng cấp vị trí p1
            core.set(p1, core.get(p1) + 1);
        }
        entity.setCoreLevelJson(toJson(core));
        repo.save(entity);
        redis.delete(String.format(LIMITCORE_KEY, userId));
    }

    private LimitCoreDTO toDto(LimitCoreEntity e) {
        LimitCoreDTO dto = new LimitCoreDTO();
        dto.setCoreLevel(parseList(e.getCoreLevelJson()));
        return dto;
    }

    private List<Integer> parseList(String json) {
        try {
            return Arrays.asList(new ObjectMapper().readValue(json, Integer[].class));
        } catch (Exception ex) {
            return Arrays.asList(0, 0, 0);
        }
    }

    private String toJson(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (Exception ex) {
            return "[0,0,0]";
        }
    }

    private LimitCoreDTO parse(String json) {
        try {
            return new ObjectMapper().readValue(json, LimitCoreDTO.class);
        } catch (Exception ex) {
            return new LimitCoreDTO();
        }
    }
}