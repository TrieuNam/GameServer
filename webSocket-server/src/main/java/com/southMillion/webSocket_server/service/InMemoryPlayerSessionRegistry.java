package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.dto.PlayerSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InMemoryPlayerSessionRegistry implements PlayerSessionRegistry {

    // wsId -> session
    private final Map<String, PlayerSession> byWs = new ConcurrentHashMap<>();

    // userId -> set(wsId)
    private final Map<String, Set<String>> byUser = new ConcurrentHashMap<>();

    // roleId -> set(wsId)  (hỗ trợ nhiều phiên cùng 1 role)
    private final Map<String, Set<String>> byRole = new ConcurrentHashMap<>();

    @Override
    public void put(PlayerSession ps) {
        String wsId = ps.getWs().getId();
        byWs.put(wsId, ps);

        // nếu session đã có sẵn user/role (trong trường hợp rebind sớm) thì gắn luôn
        if (ps.getUserId() != null) {
            byUser.computeIfAbsent(ps.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(wsId);
        }
        if (ps.getRoleId() != null) {
            byRole.computeIfAbsent(ps.getRoleId(), k -> ConcurrentHashMap.newKeySet()).add(wsId);
        }
        log.debug("[registry] put ws={}", wsId);
    }

    @Override
    public void remove(PlayerSession ps) {
        String wsId = ps.getWs().getId();
        byWs.remove(wsId);

        if (ps.getUserId() != null) {
            var set = byUser.get(ps.getUserId());
            if (set != null) {
                set.remove(wsId);
                if (set.isEmpty()) byUser.remove(ps.getUserId());
            }
        }
        if (ps.getRoleId() != null) {
            var set = byRole.get(ps.getRoleId());
            if (set != null) {
                set.remove(wsId);
                if (set.isEmpty()) byRole.remove(ps.getRoleId());
            }
        }
        log.debug("[registry] remove ws={} user={} role={}", wsId, ps.getUserId(), ps.getRoleId());
    }

    @Override
    public void bindRoleToSession(PlayerSession ps, String roleId, String userId, String roleName) {
        String wsId = ps.getWs().getId();

        // gỡ ánh xạ role cũ (nếu có)
        String oldRole = ps.getRoleId();
        if (oldRole != null && !oldRole.equals(roleId)) {
            var setOld = byRole.get(oldRole);
            if (setOld != null) {
                setOld.remove(wsId);
                if (setOld.isEmpty()) byRole.remove(oldRole);
            }
        }

        // cập nhật session
        ps.setRoleId(roleId);
        ps.setUserId(userId);
        ps.setRoleName(roleName);

        // gắn vào user/role map
        byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(wsId);
        byRole.computeIfAbsent(roleId, k -> ConcurrentHashMap.newKeySet()).add(wsId);

        log.debug("[registry] bind user={} role={} ws={}", userId, roleId, wsId);
    }

    @Override
    public Optional<PlayerSession> getByWsId(String wsId) {
        return Optional.ofNullable(byWs.get(wsId));
    }

    @Override
    public Optional<PlayerSession> getByRoleId(String roleId) {
        var ids = byRole.getOrDefault(roleId, Set.of());
        for (String wsId : ids) {
            PlayerSession ps = byWs.get(wsId);
            if (ps != null) return Optional.of(ps);
        }
        return Optional.empty();
    }

    @Override
    public List<PlayerSession> sessionsOfRole(String roleId) {
        var ids = byRole.getOrDefault(roleId, Set.of());
        List<PlayerSession> out = new ArrayList<>(ids.size());
        for (String wsId : ids) {
            var s = byWs.get(wsId);
            if (s != null) out.add(s);
        }
        return out;
    }

    @Override
    public List<PlayerSession> sessionsOfUser(String userId) {
        var ids = byUser.getOrDefault(userId, Set.of());
        List<PlayerSession> out = new ArrayList<>(ids.size());
        for (String wsId : ids) {
            var s = byWs.get(wsId);
            if (s != null) out.add(s);
        }
        return out;
    }
}