package com.SouthMillion.globalserver_service.service.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NoticeRedisService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String UNREAD_PREFIX = "notice:unread:";

    public Integer getUnreadNum(String userKey) {
        String value = redisTemplate.opsForValue().get(UNREAD_PREFIX + userKey);
        return value != null ? Integer.parseInt(value) : null;
    }

    public void setUnreadNum(String userKey, int num) {
        redisTemplate.opsForValue().set(UNREAD_PREFIX + userKey, String.valueOf(num));
    }

    public void deleteUnreadNum(String userKey) {
        redisTemplate.delete(UNREAD_PREFIX + userKey);
    }
}