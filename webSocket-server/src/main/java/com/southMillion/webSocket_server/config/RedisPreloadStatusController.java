package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.service.ConfigSnapshotLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/redis-preload")
@RequiredArgsConstructor
public class RedisPreloadStatusController {

    private final StartupConfigRedisPreloader preloader;
    private final ConfigSnapshotLookupService configLookup;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return preloader.statusSnapshot();
    }

    @GetMapping("/config")
    public Map<String, Object> config(@RequestParam("path") String path) {
        return configLookup.inspect(path);
    }
}
