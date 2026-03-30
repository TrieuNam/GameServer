package com.SouthMillion.webSocket_server.utils;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import io.micrometer.common.util.StringUtils;

public final class SessionUtils {
    private SessionUtils(){}

    public static boolean hasUser(PlayerSession ps) {
        return ps != null && StringUtils.isNotBlank(ps.getUserId());
    }
    public static boolean hasRole(PlayerSession ps) {
        return ps != null && ps.getRoleId() != null;
    }
}