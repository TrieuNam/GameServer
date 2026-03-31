package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.service.LoginSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/login-snapshot")
@RequiredArgsConstructor
public class LoginSnapshotStatusController {

    private final LoginSnapshotService loginSnapshotService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return loginSnapshotService.status();
    }

    @GetMapping("/role/{roleId}")
    public Map<String, Object> role(@PathVariable("roleId") Long roleId) {
        return loginSnapshotService.roleSnapshotStatus(roleId);
    }
}
