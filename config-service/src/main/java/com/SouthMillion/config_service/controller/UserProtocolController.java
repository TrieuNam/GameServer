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

    @Data
    @AllArgsConstructor
    static class Resp<T> {
        private int ret;
        private T data;
        static <T> Resp<T> ok(T data){ return new Resp<>(0, data); }
    }
}