package com.southMillion.webSocket_server.utils;

import com.southMillion.webSocket_server.dto.PlayerSession;
import io.micrometer.common.util.StringUtils;

public final class SessionUtils {
    private SessionUtils(){}

    public static String token(PlayerSession ps) { return ps != null ? ps.getSessionId() : null; }
    public static boolean hasRole(PlayerSession ps) { return ps != null && io.micrometer.common.util.StringUtils.isNotBlank(ps.getRoleId()); }

    /** Trả về level đang cache trong session, hoặc mặc định nếu chưa có */
    public static int roleLevelOrDefault(PlayerSession ps, int def) {
        if (ps != null && ps.getRoleLevel() != null && ps.getRoleLevel() > 0) {
            return ps.getRoleLevel();
        }
        return def;
    }
}