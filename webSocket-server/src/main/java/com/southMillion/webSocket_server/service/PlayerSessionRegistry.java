package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.dto.PlayerSession;

import java.util.List;
import java.util.Optional;

public interface PlayerSessionRegistry {
    void put(PlayerSession ps);                     // thêm session khi kết nối
    void remove(PlayerSession ps);                  // xoá khi disconnect

    void bindRoleToSession(PlayerSession ps, String roleId, String userId, String roleName);

    // Lấy 1 phiên bất kỳ của role (tiện cho emit "first found")
    Optional<PlayerSession> getByRoleId(String roleId);

    // Lấy tất cả phiên của role (đa thiết bị thì broadcast)
    List<PlayerSession> sessionsOfRole(String roleId);

    // Lấy theo user
    List<PlayerSession> sessionsOfUser(String userId);

    // Lấy theo ws id (tiện debug)
    Optional<PlayerSession> getByWsId(String wsId);
}
