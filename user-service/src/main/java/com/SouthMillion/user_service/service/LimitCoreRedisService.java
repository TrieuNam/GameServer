package com.SouthMillion.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LimitCoreRedisService {
    private static final String PREFIX = "limit_core:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setCoreLevels(Long userId, List<Integer> coreLevels) {
        redisTemplate.opsForValue().set(PREFIX + userId, coreLevels, Duration.ofHours(12));
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCoreLevels(Long userId) {
        Object val = redisTemplate.opsForValue().get(PREFIX + userId);
        return val == null ? null : (List<Integer>) val;
    }
}