package com.southMillion.session_service.service;

import com.southMillion.session_service.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final RateLimitProperties props;

    // Dùng ms để chính xác cao, member là now-nonce để không đụng độ
    private static final String LUA = String.join("\n",
            "local key=KEYS[1]",
            "local now=tonumber(ARGV[1])",
            "local window=tonumber(ARGV[2])",
            "local limit=tonumber(ARGV[3])",
            "local nonce=ARGV[4]",
            "redis.call('ZREMRANGEBYSCORE', key, 0, now-window)",
            "local cnt=redis.call('ZCARD', key)",
            "if cnt >= limit then",
            "  local earliest=redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')",
            "  local reset=0",
            "  if earliest and earliest[2] then reset = tonumber(earliest[2]) + window end",
            "  return {0, cnt, reset}",
            "end",
            "redis.call('ZADD', key, now, tostring(now) .. '-' .. nonce)",
            "redis.call('PEXPIRE', key, window)",
            "cnt=cnt+1",
            "return {1, cnt, now+window}"
    );

    private static final DefaultRedisScript<List> SCRIPT;
    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(LUA);
        SCRIPT.setResultType(List.class);
    }

    public record Decision(boolean allowed, long count, long resetAtEpochSec) {}

    public Decision allow(String bucketKey, int limit, int windowSec) {
        if (!props.isEnabled()) {
            long reset = Instant.now().getEpochSecond() + windowSec;
            return new Decision(true, 0, reset);
        }

        long nowMs = System.currentTimeMillis();
        long windowMs = windowSec * 1000L;
        String nonce = UUID.randomUUID().toString().substring(0, 8);

        @SuppressWarnings("unchecked")
        List<Object> r = (List<Object>) redis.execute(
                SCRIPT,
                Collections.singletonList(bucketKey),
                String.valueOf(nowMs),
                String.valueOf(windowMs),
                String.valueOf(limit),
                nonce
        );

        if (r == null || r.size() < 3) {
            // fallback an toàn: cho qua 1 request
            long reset = (nowMs + windowMs + 999) / 1000;
            return new Decision(true, 1, reset);
        }

        long allowedFlag = ((Number) r.get(0)).longValue();
        long count       = ((Number) r.get(1)).longValue();
        long resetMs     = ((Number) r.get(2)).longValue();

        long resetEpochSec = (resetMs + 999) / 1000; // làm tròn lên giây
        return new Decision(allowedFlag == 1L, count, resetEpochSec);
    }
}