package com.SouthMillion.activity_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableRetry
@RestController
public class ActivityServiceApplication {

    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ActivityServiceApplication() {
        scheduler.scheduleAtFixedRate(this::evictOldEntries, 1, 1, TimeUnit.HOURS);
    }

    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }

    @RequestMapping(value = "/battle/turnOrder", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> calculateTurnOrder(@RequestBody(required = false) Map<String, Object> battleRequest) {
        // Keep a safe stub endpoint so the app compiles until real battle DTOs are introduced.
        return ResponseEntity.ok(Map.of(
            "ok", true,
            "message", "turn order endpoint placeholder",
            "requestReceived", battleRequest != null
        ));
    }

    private void evictOldEntries() {
        // Logic for eviction or TTL handling
        // For example: iterate over the cache keys and remove old entries based on last accessed time
    }
}