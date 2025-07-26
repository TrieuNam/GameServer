package com.SouthMillion.gameworld_service.service.cache;

import org.SouthMillion.dto.gameworld.MainFbDTO;
import org.SouthMillion.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MainFbCache {
    @Autowired
    private StringRedisTemplate redis;
    private static final String KEY_FMT = "mainfb:user:%d";

    public void save(MainFbDTO dto) {
        String key = String.format(KEY_FMT, dto.getUserId());
        redis.opsForValue().set(key, JsonUtil.toJson(dto), Duration.ofHours(2));
    }

    public MainFbDTO get(Long userId) {
        String key = String.format(KEY_FMT, userId);
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        return JsonUtil.fromJson(json, MainFbDTO.class);
    }

    public void evict(Long userId) {
        redis.delete(String.format(KEY_FMT, userId));
    }
}
