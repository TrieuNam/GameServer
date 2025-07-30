package com.southMillion.session_service.config;

import org.SouthMillion.dto.session.SessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class SessionRedits {
    private static final String SESSION_PREFIX = "session:";

    @Autowired
    private RedisTemplate<String, SessionDto> redisTemplate;

    public void save(SessionDto session) {
        String key = SESSION_PREFIX + session.getSessionId();
        redisTemplate.opsForValue().set(key, session, 2, TimeUnit.HOURS); // TTL 2h
    }

    public SessionDto findBySessionId(String sessionId) {
        return redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
    }

    public void remove(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    public SessionDto findByRoleId(Integer roleId) {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                SessionDto dto = redisTemplate.opsForValue().get(key);
                if (dto != null && dto.getRoleId() != null && dto.getRoleId().equals(roleId)) {
                    return dto;
                }
            }
        }
        return null;
    }
}