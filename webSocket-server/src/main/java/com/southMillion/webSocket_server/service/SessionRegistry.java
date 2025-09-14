package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.dto.PlayerSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry {
    private final Map<String, PlayerSession> byWs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> byUser = new ConcurrentHashMap<>();

    public void put(String id, PlayerSession ps) { byWs.put(ps.getWs().getId(), ps); }

    public void remove(PlayerSession ps) {
        byWs.remove(ps.getWs().getId());
        if (ps.getUserId() != null) {
            var set = byUser.get(ps.getUserId());
            if (set != null) {
                set.remove(ps.getWs().getId());
                if (set.isEmpty()) byUser.remove(ps.getUserId());
            }
        }
    }

    public void bindRoleToSession(PlayerSession ps, String roleId, String userId, String roleName) {
        ps.setRoleId(roleId);
        ps.setUserId(userId);
        ps.setRoleName(roleName);
        byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(ps.getWs().getId());
        log.debug("[registry] bind user={} role={} ws={}", userId, roleId, ps.getWs().getId());
    }

    public List<PlayerSession> sessionsOfUser(String userId) {
        var ids = byUser.getOrDefault(userId, Set.of());
        List<PlayerSession> out = new ArrayList<>(ids.size());
        for (var id : ids) {
            var s = byWs.get(id);
            if (s != null) out.add(s);
        }
        return out;
    }
}