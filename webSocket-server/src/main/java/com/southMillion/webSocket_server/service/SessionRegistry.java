package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.dto.PlayerSession;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Quản lý mọi WebSocket PlayerSession:
 *  - Tra cứu theo wsId
 *  - Gom nhóm theo userId / roleId
 *  - Cho phép cập nhật ràng buộc khi đăng nhập xong (userId/roleId có thể đến muộn sau khi mở WS)
 */
@Component
public class SessionRegistry {

    // wsId -> PlayerSession
    private final ConcurrentHashMap<String, PlayerSession> byWs = new ConcurrentHashMap<>();

    // userId -> set of wsId
    private final ConcurrentHashMap<String, Set<String>> wsByUser = new ConcurrentHashMap<>();
    // roleId -> set of wsId
    private final ConcurrentHashMap<String, Set<String>> wsByRole = new ConcurrentHashMap<>();

    // đảo chiều để gỡ ràng buộc cũ khi update
    private final ConcurrentHashMap<String, String> userByWs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roleByWs = new ConcurrentHashMap<>();

    public void put(String wsId, PlayerSession ps) {
        byWs.put(wsId, ps);
        // Chưa bind gì khi mới mở WS (chưa login), bind sau bằng updateBindings(ps)
    }

    public PlayerSession get(String wsId) {
        return byWs.get(wsId);
    }

    public int connectedCount() {
        return byWs.size();
    }

    public void remove(String wsId) {
        byWs.remove(wsId);

        // tháo ràng buộc user
        String oldUser = userByWs.remove(wsId);
        if (oldUser != null) {
            removeFrom(wsByUser, oldUser, wsId);
        }

        // tháo ràng buộc role
        String oldRole = roleByWs.remove(wsId);
        if (oldRole != null) {
            removeFrom(wsByRole, oldRole, wsId);
        }
    }

    /** Bind lại theo thông tin hiện tại trong PlayerSession (userId/roleId có thể thay đổi sau login/chọn nhân vật) */
    public void updateBindings(PlayerSession ps) {
        String wsId = ps.getWs().getId();

        // ---- userId
        String newUser = emptyToNull(ps.getUserId());
        String oldUser = userByWs.get(wsId);

        if (!Objects.equals(oldUser, newUser)) {
            // gỡ cũ
            if (oldUser != null) {
                removeFrom(wsByUser, oldUser, wsId);
            }
            // set mới
            if (newUser != null) {
                addTo(wsByUser, newUser, wsId);
                userByWs.put(wsId, newUser);
            } else {
                userByWs.remove(wsId);
            }
        }

        // ---- roleId
        String newRole = emptyToNull(ps.getRoleId());
        String oldRole = roleByWs.get(wsId);

        if (!Objects.equals(oldRole, newRole)) {
            if (oldRole != null) {
                removeFrom(wsByRole, oldRole, wsId);
            }
            if (newRole != null) {
                addTo(wsByRole, newRole, wsId);
                roleByWs.put(wsId, newRole);
            } else {
                roleByWs.remove(wsId);
            }
        }
    }

    public List<PlayerSession> sessionsOfUser(String userId) {
        Set<String> ids = wsByUser.getOrDefault(userId, Collections.emptySet());
        return ids.stream().map(byWs::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<PlayerSession> sessionsOfRole(String roleId) {
        Set<String> ids = wsByRole.getOrDefault(roleId, Collections.emptySet());
        return ids.stream().map(byWs::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // ===== helpers
    private static void addTo(Map<String, Set<String>> idx, String key, String wsId) {
        idx.compute(key, (_k, set) -> {
            if (set == null) set = ConcurrentHashMap.newKeySet();
            set.add(wsId);
            return set;
        });
    }

    private static void removeFrom(Map<String, Set<String>> idx, String key, String wsId) {
        idx.computeIfPresent(key, (_k, set) -> {
            set.remove(wsId);
            return set.isEmpty() ? null : set;
        });
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}