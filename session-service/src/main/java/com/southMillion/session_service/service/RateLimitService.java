package com.southMillion.session_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;
    @Value("${ratelimit.window-seconds:60}")
    private long windowSeconds;
    @Value("${ratelimit.login-limit:10}")
    private long loginLimit;
    @Value("${ratelimit.refresh-limit:60}")
    private long refreshLimit;
    @Value("${ratelimit.generic-limit:300}")
    private long genericLimit;

    private static final DefaultRedisScript<Long> INCR_EXPIRE = new DefaultRedisScript<>(
            "local c=redis.call('INCR', KEYS[1]); if c==1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; return c",
            Long.class
    );

    public void checkLogin(String ip, String username) {
        check("rl:login:" + ip + ":" + username, loginLimit);
    }

    public void checkRefresh(String userId) {
        check("rl:refresh:" + userId, refreshLimit);
    }

    public void checkGeneric(String ip) {
        check("rl:generic:" + ip, genericLimit);
    }

    private void check(String key, long limit) {
        if (!enabled) return;
        Long c = redis.execute(INCR_EXPIRE, List.of(key), String.valueOf(windowSeconds));
        if (c != null && c > limit) {
            throw new IllegalArgumentException("rate_limited");
        }
    }
}