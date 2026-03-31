package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.service.UserProtocolService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/c2s")
public class UserProtocolController {

    private final UserProtocolService userProtocolService;

    public UserProtocolController(UserProtocolService s) { this.userProtocolService = s; }

    @GetMapping("/fetch_privacy_notice")
    public ResponseEntity<Resp<String>> fetchPrivacyNotice(@RequestParam(value = "spid", required = false) String spid) {
        String notice = userProtocolService.getPrivacyNotice();
        return ResponseEntity.ok(Resp.ok(notice));
    }

    /**
     * Role info reporting endpoint — called by client after OnRoleInfoAck.
     * Accepts player/role info for analytics purposes and returns a success response.
     */
    @GetMapping("/user_info")
    public ResponseEntity<Resp<String>> userInfo(
            @RequestParam(value = "spid",       required = false) String spid,
            @RequestParam(value = "server_id",  required = false) String serverId,
            @RequestParam(value = "user_id",    required = false) String userId,
            @RequestParam(value = "role_id",    required = false) String roleId,
            @RequestParam(value = "role_name",  required = false) String roleName,
            @RequestParam(value = "level",      required = false, defaultValue = "0") int level,
            @RequestParam(value = "vip",        required = false, defaultValue = "0") int vip) {
        return ResponseEntity.ok(Resp.ok("ok"));
    }

    @Data
    @AllArgsConstructor
    static class Resp<T> {
        private int ret;
        private T data;
        static <T> Resp<T> ok(T data){ return new Resp<>(0, data); }
    }
}