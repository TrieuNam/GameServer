package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.handler.login.LoginBootstrapHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDependencyReadiness {

    private final DiscoveryClient discoveryClient;
    private final AtomicBoolean monitorStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor(readinessThreadFactory());

    private volatile boolean currentReady = true;

    @Value("${app.bootstrap.readiness.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.readiness.max-attempts:8}")
    private int maxAttempts;

    @Value("${app.bootstrap.readiness.initial-delay-ms:400}")
    private long initialDelayMs;

    @Value("${app.bootstrap.readiness.max-delay-ms:3000}")
    private long maxDelayMs;

    @Value("${app.bootstrap.readiness.recheck-interval-ms:5000}")
    private long recheckIntervalMs;

    @Value("${app.bootstrap.readiness.required-services:role-service,world-service,bag-service}")
    private List<String> requiredServices;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyCriticalDependencies() {
        if (!enabled) {
            log.info("[startup] readiness check disabled");
            updateWorldReady(true, "readiness check disabled");
            return;
        }

        updateWorldReady(false, "startup verification in progress");
        long delay = Math.max(100L, initialDelayMs);

        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            List<String> missing = findMissingServices();
            if (missing.isEmpty()) {
                updateWorldReady(true, "startup dependencies discovered");
                log.info("[startup] readiness OK after attempt {}/{}: {}",
                        attempt, maxAttempts, requiredServices);
                ensureMonitorRunning();
                return;
            }

            log.warn("[startup] readiness pending attempt {}/{} missing={} nextDelayMs={}",
                    attempt, maxAttempts, missing, delay);
            if (attempt < maxAttempts) {
                sleepQuietly(delay);
                delay = Math.min(Math.max(delay * 2, delay), Math.max(delay, maxDelayMs));
            }
        }

        log.error("[startup] readiness failed after {} attempts, login stays blocked until dependencies recover", maxAttempts);
        ensureMonitorRunning();
    }

    @PreDestroy
    public void shutdownMonitor() {
        monitorExecutor.shutdownNow();
    }

    private void ensureMonitorRunning() {
        if (!enabled || !monitorStarted.compareAndSet(false, true)) {
            return;
        }

        long intervalMs = Math.max(1_000L, recheckIntervalMs);
        monitorExecutor.scheduleWithFixedDelay(this::refreshReadinessSafely,
                intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("[startup] readiness background monitor enabled intervalMs={}", intervalMs);
    }

    private void refreshReadinessSafely() {
        try {
            List<String> missing = findMissingServices();
            if (missing.isEmpty()) {
                updateWorldReady(true, "dependencies healthy");
            } else {
                updateWorldReady(false, "missing=" + missing);
            }
        } catch (Exception ex) {
            updateWorldReady(false, "readiness monitor exception=" + ex.getClass().getSimpleName());
            log.warn("[startup] readiness monitor error: {}", ex.toString());
        }
    }

    private void updateWorldReady(boolean ready, String reason) {
        LoginBootstrapHandler.setWorldReady(ready);
        if (currentReady == ready) {
            return;
        }
        currentReady = ready;
        if (ready) {
            log.info("[startup] login readiness OPEN ({})", reason);
        } else {
            log.warn("[startup] login readiness BLOCKED ({})", reason);
        }
    }

    private List<String> findMissingServices() {
        List<String> missing = new ArrayList<>();
        if (requiredServices == null || requiredServices.isEmpty()) {
            return missing;
        }

        for (String service : requiredServices) {
            if (service == null || service.isBlank()) continue;
            try {
                if (discoveryClient.getInstances(service).isEmpty()) {
                    missing.add(service);
                }
            } catch (Exception ex) {
                missing.add(service);
            }
        }
        return missing;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private ThreadFactory readinessThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "startup-readiness-monitor");
            thread.setDaemon(true);
            return thread;
        };
    }
}