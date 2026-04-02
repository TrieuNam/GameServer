package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.config.ScheduledConfigDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for monitoring and controlling the scheduled config data loader.
 *
 * Endpoints:
 * - GET  /api/scheduled-config-loader/status  - Get current loader status
 * - POST /api/scheduled-config-loader/trigger - Manually trigger config data load
 */
@RestController
@RequestMapping("/api/scheduled-config-loader")
@RequiredArgsConstructor
public class ScheduledConfigLoaderStatusController {

    private final ScheduledConfigDataLoader scheduledConfigDataLoader;

    /**
     * Get current status of the scheduled config data loader.
     *
     * Returns:
     * - enabled: whether the scheduled loader is enabled
     * - scheduleIntervalMinutes: how often the loader runs
     * - lastRunTime: timestamp of last successful run
     * - successCount: number of successfully loaded config files in last run
     * - failureCount: number of failed config files in last run
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = scheduledConfigDataLoader.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Manually trigger config data load.
     * Useful for forcing a refresh without waiting for the next scheduled run.
     *
     * Note: This will run synchronously and may take several seconds.
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerLoad() {
        long startTime = System.currentTimeMillis();

        try {
            scheduledConfigDataLoader.loadConfigData();
            long elapsed = System.currentTimeMillis() - startTime;

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Config data load triggered successfully",
                    "elapsedMs", elapsed
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to trigger config data load",
                    "error", e.getMessage(),
                    "elapsedMs", elapsed
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Check if config data is available in Redis.
     * Returns simple boolean indicating whether critical config data exists.
     */
    @GetMapping("/data-available")
    public ResponseEntity<Map<String, Object>> checkDataAvailable() {
        boolean available = scheduledConfigDataLoader.isConfigDataAvailable();

        Map<String, Object> response = Map.of(
                "available", available,
                "message", available
                        ? "Config data is available in Redis"
                        : "Config data is missing or incomplete in Redis"
        );

        return ResponseEntity.ok(response);
    }
}
