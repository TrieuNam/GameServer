package com.SouthMillion.scheduler_service.controller;

import com.SouthMillion.scheduler_service.job.DailyResetJob;
import com.SouthMillion.scheduler_service.job.WeeklyResetJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final DailyResetJob dailyResetJob;
    private final WeeklyResetJob weeklyResetJob;

    @PostMapping("/trigger/daily-reset")
    public ResponseEntity<Map<String, Object>> triggerDailyReset() {
        log.info("[Scheduler] Manual daily reset triggered");
        try {
            dailyResetJob.executeDailyReset();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily reset executed",
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("[Scheduler] Daily reset trigger failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Daily reset failed: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    @PostMapping("/trigger/weekly-reset")
    public ResponseEntity<Map<String, Object>> triggerWeeklyReset() {
        log.info("[Scheduler] Manual weekly reset triggered");
        try {
            weeklyResetJob.executeWeeklyReset();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Weekly reset executed",
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("[Scheduler] Weekly reset trigger failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Weekly reset failed: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "service", "scheduler-service",
            "status", "running",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
