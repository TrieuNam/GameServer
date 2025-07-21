package com.southMillion.serverInfo_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.serverInfor.ServerInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    private static final String SERVER_INFO_KEY = "server_info";

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void setServerInfo(ServerInfoDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(SERVER_INFO_KEY, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ServerInfoDto getServerInfo() {
        try {
            String json = redisTemplate.opsForValue().get(SERVER_INFO_KEY);
            if (json == null) return null;
            return objectMapper.readValue(json, ServerInfoDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void evictServerInfo() {
        redisTemplate.delete(SERVER_INFO_KEY);
    }
}