package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.handler.login.LoginBootstrapHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartupDependencyReadiness.class);

    private final DiscoveryClient discoveryClient;
    private final StartupConfigRedisPreloader configRedisPreloader;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean monitorStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor(readinessThreadFactory());
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

    @Value("${app.bootstrap.readiness.required-services:role-service,world-service,bag-service,config-service,drop-service}")
    private List<String> requiredServices;

    @Value("${app.bootstrap.readiness.check-health-endpoints:true}")
    private boolean checkHealthEndpoints;

    @Value("${app.bootstrap.readiness.health-timeout-ms:1500}")
    private long healthTimeoutMs;

    @Value("${app.bootstrap.readiness.health-path:/actuator/health}")
    private String healthPath;

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
                if (ensureConfigWarmup("startup-attempt-" + attempt)) {
                    updateWorldReady(true, "startup dependencies discovered + redis preload ready");
                    log.info("[startup] readiness OK after attempt {}/{}: {}",
                            attempt, maxAttempts, requiredServices);
                    ensureMonitorRunning();
                    return;
                }
                log.warn("[startup] dependencies discovered but Redis config preload not ready yet (attempt {}/{})",
                        attempt, maxAttempts);
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
                boolean preloadReady = ensureConfigWarmup("background-monitor");
                if (preloadReady) {
                    updateWorldReady(true, "dependencies healthy + redis preload ready");
                } else {
                    log.debug("[startup] readiness monitor waiting for redis preload to finish");
                    updateWorldReady(false, "redis preload incomplete");
                }
            } else {
                log.debug("[startup] readiness monitor still missing {}", missing);
                updateWorldReady(false, "missing=" + missing);
            }
        } catch (Exception ex) {
            updateWorldReady(false, "readiness monitor exception=" + ex.getClass().getSimpleName());
            log.warn("[startup] readiness monitor error: {}", ex.toString());
        }
    }

    private boolean ensureConfigWarmup(String reason) {
        try {
            if (configRedisPreloader.isReadyForLogin()) {
                return true;
            }
            log.info("[startup] triggering redis config warmup ({})", reason);
            return configRedisPreloader.reloadNow();
        } catch (Exception ex) {
            log.warn("[startup] redis warmup failed ({}): {}", reason, ex.toString());
            return false;
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
                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (instances.isEmpty()) {
                    missing.add(service + "(not-discovered)");
                    continue;
                }
                if (checkHealthEndpoints) {
                    String healthIssue = firstHealthIssue(service, instances);
                    if (healthIssue != null) {
                        missing.add(healthIssue);
                    }
                }
            } catch (Exception ex) {
                missing.add(service + "(lookup-error=" + ex.getClass().getSimpleName() + ")");
            }
        }
        return missing;
    }

    private String firstHealthIssue(String service, List<ServiceInstance> instances) {
        String lastIssue = null;
        for (ServiceInstance instance : instances) {
            String issue = probeHealth(service, instance);
            if (issue == null) {
                return null;
            }
            lastIssue = issue;
        }
        return lastIssue != null ? lastIssue : service + "(health=UNKNOWN)";
    }

    private String probeHealth(String service, ServiceInstance instance) {
        URI baseUri = instance.getUri();
        String path = (healthPath == null || healthPath.isBlank()) ? "/actuator/health" : healthPath;

        String issue = probeHealthUri(service, buildHealthUri(baseUri, path));
        if (issue == null) {
            return null;
        }

        URI managementHealthUri = buildManagementHealthUri(instance, baseUri, path);
        if (managementHealthUri != null) {
            String managementIssue = probeHealthUri(service, managementHealthUri);
            if (managementIssue == null) {
                log.debug("[startup] health probe for {} succeeded via management port uri={}", service, managementHealthUri);
                return null;
            }
            issue = managementIssue;
        }

        return issue;
    }

    private String probeHealthUri(String service, URI healthUri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(healthUri)
                    .timeout(Duration.ofMillis(Math.max(300L, healthTimeoutMs)))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404 && "/actuator/health".equalsIgnoreCase(healthUri.getPath())) {
                log.debug("[startup] {} has no actuator health endpoint at {}, treating discovered instance as reachable",
                        service, healthUri);
                return null;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return service + "(health-http=" + response.statusCode() + ")";
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText("UNKNOWN");
            if ("UP".equalsIgnoreCase(status)) {
                return null;
            }

            String detail = extractHealthDetail(root);
            return service + "(health=" + status + (detail.isBlank() ? "" : ", " + detail) + ")";
        } catch (Exception ex) {
            return service + "(health-error=" + ex.getClass().getSimpleName() + ")";
        }
    }

    private URI buildHealthUri(URI baseUri, String path) {
        return URI.create(baseUri.toString().replaceAll("/$", "") + (path.startsWith("/") ? path : "/" + path));
    }

    private URI buildManagementHealthUri(ServiceInstance instance, URI baseUri, String path) {
        try {
            String managementPort = instance.getMetadata().get("management.port");
            if (managementPort == null || managementPort.isBlank()) {
                return null;
            }
            int port = Integer.parseInt(managementPort.trim());
            if (port <= 0 || port == baseUri.getPort()) {
                return null;
            }
            return URI.create(baseUri.getScheme() + "://" + baseUri.getHost() + ":" + port + (path.startsWith("/") ? path : "/" + path));
        } catch (Exception ex) {
            log.debug("[startup] ignore invalid management.port metadata for {}: {}", instance.getServiceId(), ex.toString());
            return null;
        }
    }

    private String extractHealthDetail(JsonNode root) {
        JsonNode dropRedis = root.path("components").path("dropRedis");
        if (dropRedis.isMissingNode() || dropRedis.isNull()) {
            return "";
        }
        JsonNode details = dropRedis.path("details");
        int missingCount = details.path("missingCount").asInt(-1);
        JsonNode sample = details.path("missingSample");
        if (missingCount < 0) {
            return "";
        }
        if (sample.isArray() && !sample.isEmpty()) {
            return "missingCount=" + missingCount + " sample=" + sample.toString();
        }
        return "missingCount=" + missingCount;
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