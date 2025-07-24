package com.SouthMillion.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NoticeTimeRedisService {
    private static final String PREFIX = "notice_time:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setNoticeTime(Long userId, Long noticeTime) {
        redisTemplate.opsForValue().set(PREFIX + userId, noticeTime, Duration.ofHours(12));
    }

    public Long getNoticeTime(Long userId) {
        Object val = redisTemplate.opsForValue().get(PREFIX + userId);
        return val == null ? null : Long.valueOf(val.toString());
    }
}
