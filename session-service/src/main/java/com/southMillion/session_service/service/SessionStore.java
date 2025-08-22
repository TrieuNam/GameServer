package com.southMillion.session_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class SessionStore {
    private final StringRedisTemplate redis;

    private String keySession(String sid){ return "sess:"+sid; }
    private String keyUserIndex(String uid){ return "sess_user:"+uid; }

    private String sha256(String s){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e){ throw new RuntimeException(e); }
    }

    public void saveSession(String sid, String userId, String username, String refreshToken, long ttlSeconds, String ip, String ua) {
        String k = keySession(sid);
        Map<String,String> m = new HashMap<>();
        m.put("uid", userId);
        m.put("uname", username);
        m.put("rt_hash", sha256(refreshToken));
        m.put("created_at", String.valueOf(Instant.now().getEpochSecond()));
        m.put("ip", ip==null?"":ip);
        m.put("ua", ua==null?"":ua);
        redis.opsForHash().putAll(k, m);
        redis.expire(k, ttlSeconds, TimeUnit.SECONDS);
        redis.opsForSet().add(keyUserIndex(userId), sid);
        redis.expire(keyUserIndex(userId), ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean verifyRefresh(String sid, String refreshToken){
        String k = keySession(sid);
        Object hash = redis.opsForHash().get(k, "rt_hash");
        return hash != null && hash.toString().equals(sha256(refreshToken));
    }

    public Map<Object,Object> get(String sid){
        return redis.opsForHash().entries(keySession(sid));
    }

    public void updateRefresh(String sid, String newRefreshToken, long ttlSeconds){
        redis.opsForHash().put(keySession(sid), "rt_hash", sha256(newRefreshToken));
        redis.expire(keySession(sid), ttlSeconds, TimeUnit.SECONDS);
    }

    public void delete(String sid){
        Map<Object,Object> m = get(sid);
        redis.delete(keySession(sid));
        Object uid = m.get("uid");
        if (uid != null) redis.opsForSet().remove(keyUserIndex(uid.toString()), sid);
    }

    public Set<String> listByUser(String uid){
        Set<String> members = redis.opsForSet().members(keyUserIndex(uid));
        return members==null? Collections.emptySet(): members;
    }

    public void revokeAll(String uid){
        Set<String> sids = listByUser(uid);
        if (sids!=null) sids.forEach(this::delete);
    }
}