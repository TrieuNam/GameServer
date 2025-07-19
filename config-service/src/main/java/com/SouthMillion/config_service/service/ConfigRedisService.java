package com.SouthMillion.config_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class ConfigRedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private static final String CONFIG_PREFIX = "config:";
    private static final Duration CACHE_TTL = Duration.ofDays(3);

    @Autowired
    private ObjectMapper objectMapper;


    public JsonNode getConfig(String name) {
        String json = redisTemplate.opsForValue().get(CONFIG_PREFIX + name);
        if (json == null) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            // Log lỗi nếu cần
            return null;
        }
    }

    public void setConfig(String name, JsonNode node) {
        try {
            String json = objectMapper.writeValueAsString(node);
            redisTemplate.opsForValue().set(CONFIG_PREFIX + name, json, CACHE_TTL);
        } catch (Exception e) {
            // Log lỗi nếu cần
        }
    }

    public Set<String> listAllKeys() {
        return redisTemplate.keys(CONFIG_PREFIX + "*");
    }

    public void deleteAll() {
        Set<String> keys = listAllKeys();
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}