package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.service.UserProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/c2s")
public class UserProtocolController {

    @Autowired
    private UserProtocolService userProtocolService;

    @CrossOrigin(origins = "*")
    @GetMapping("/fetch_privacy_notice")
    public ResponseEntity<String> fetchPrivacyNotice(@RequestParam(value = "spid", required = false) String spid) {
        // Có thể dùng spid để trả về bản privacy riêng cho từng spid nếu muốn
        String notice = userProtocolService.getPrivacyNotice();
        return ResponseEntity.ok(notice);
    }
}