package com.SouthMillion.admin.controller;

import com.SouthMillion.admin.dto.ServiceControlResponse;
import com.SouthMillion.admin.dto.ServiceInfo;
import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import com.SouthMillion.admin.repository.ServiceConfigRepository;
import com.SouthMillion.admin.service.ServiceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service Control REST API
 * Provides endpoints for managing GameServer microservices
 */
@RestController
@RequestMapping("/api/services")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final ServiceManager serviceManager;
    private final ServiceConfigRepository configRepository;
    private final com.SouthMillion.admin.service.DockerManager dockerManager;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * Get all services
     */
    @GetMapping
    public ResponseEntity<List<ServiceInfo>> getAllServices() {
        List<ServiceConfig> configs = configRepository.findAllByStartupOrder();

        List<ServiceInfo> services = configs.stream()
                .map(this::toServiceInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(services);
    }

    /**
     * Get service by name
     */
    @GetMapping("/{serviceName}")
    public ResponseEntity<ServiceInfo> getService(@PathVariable String serviceName) {
        return configRepository.findByServiceName(serviceName)
                .map(this::toServiceInfo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get services by phase
     */
    @GetMapping("/phase/{phase}")
    public ResponseEntity<List<ServiceInfo>> getServicesByPhase(@PathVariable String phase) {
        List<ServiceConfig> configs = configRepository.findByPhaseOrderByStartupOrder(phase);

        List<ServiceInfo> services = configs.stream()
                .map(this::toServiceInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(services);
    }

    /**
     * Get service status
     */
    @GetMapping("/{serviceName}/status")
    public ResponseEntity<ServiceControlResponse> getStatus(@PathVariable String serviceName) {
        ServiceStatus status = serviceManager.getServiceStatus(serviceName);

        return configRepository.findByServiceName(serviceName)
                .map(config -> ServiceControlResponse.builder()
                        .serviceName(serviceName)
                        .status(status)
                        .processId(config.getProcessId())
                        .port(config.getPort())
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start a service
     */
    @PostMapping("/{serviceName}/start")
    public ResponseEntity<ServiceControlResponse> startService(@PathVariable String serviceName) {
        log.info("🚀 API request to start service: {}", serviceName);

        CompletableFuture<Boolean> future = serviceManager.startService(serviceName);

        // Return immediate response (async operation)
        return configRepository.findByServiceName(serviceName)
                .map(config -> ServiceControlResponse.builder()
                        .serviceName(serviceName)
                        .status(ServiceStatus.STARTING)
                        .message("Service start initiated")
                        .success(true)
                        .port(config.getPort())
                        .timestamp(LocalDateTime.now())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Stop a service
     */
    @PostMapping("/{serviceName}/stop")
    public ResponseEntity<ServiceControlResponse> stopService(@PathVariable String serviceName) {
        log.info("🛑 API request to stop service: {}", serviceName);

        boolean success = serviceManager.stopService(serviceName);

        return configRepository.findByServiceName(serviceName)
                .map(config -> ServiceControlResponse.builder()
                        .serviceName(serviceName)
                        .status(ServiceStatus.STOPPED)
                        .message(success ? "Service stopped successfully" : "Failed to stop service")
                        .success(success)
                        .port(config.getPort())
                        .timestamp(LocalDateTime.now())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Restart a service
     */
    @PostMapping("/{serviceName}/restart")
    public ResponseEntity<ServiceControlResponse> restartService(@PathVariable String serviceName) {
        log.info("🔄 API request to restart service: {}", serviceName);

        serviceManager.restartService(serviceName);

        return configRepository.findByServiceName(serviceName)
                .map(config -> ServiceControlResponse.builder()
                        .serviceName(serviceName)
                        .status(ServiceStatus.STARTING)
                        .message("Service restarting")
                        .success(true)
                        .port(config.getPort())
                        .timestamp(LocalDateTime.now())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get service logs
     */
    @GetMapping("/{serviceName}/logs")
    public ResponseEntity<Map<String, Object>> getServiceLogs(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "100") int lines) {
        log.info("📋 API request to get logs for service: {} (last {} lines)", serviceName, lines);

        List<String> logs = serviceManager.getServiceLogs(serviceName, lines);
        
        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", serviceName);
        response.put("lines", logs.size());
        response.put("logs", logs);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Start all services
     */
    @PostMapping("/start-all")
    public ResponseEntity<ServiceControlResponse> startAllServices() {
        log.info("🚀 API request to start ALL services");

        serviceManager.startAllServices();

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("ALL")
                .status(ServiceStatus.STARTING)
                .message("Starting all services in priority order")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Stop all services
     */
    @PostMapping("/stop-all")
    public ResponseEntity<ServiceControlResponse> stopAllServices() {
        log.info("🛑 API request to stop ALL services");

        Map<String, Boolean> results = serviceManager.stopAllServices();

        long successCount = results.values().stream().filter(b -> b).count();

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("ALL")
                .status(ServiceStatus.STOPPED)
                .message(String.format("Stopped %d/%d services", successCount, results.size()))
                .success(successCount == results.size())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Start services by phase (P0, P1, P2, P3, P4)
     * LOGIC:
     * - Tất cả phases đều được phép start ở mọi profile (local/prod)
     * - P0: eureka-server, gateway, config → phải start đầu tiên ở local
     *   (Redis/Kafka được start tự động trong ServiceManager khi eureka-server khởi động)
     * - Start tất cả Docker DB containers của phase trước, rồi mới start Java services
     */
    @PostMapping("/phase/{phase}/start")
    public ResponseEntity<ServiceControlResponse> startPhase(@PathVariable String phase) {
        log.info("🚀 API request to start phase: {} (profile: {})", phase, activeProfile);


        List<ServiceConfig> services = configRepository.findByPhaseOrderByStartupOrder(phase);
        
        // STEP 1: Collect all Docker containers needed for this phase
        List<String> containersToStart = services.stream()
                .filter(ServiceConfig::getEnabled)
                .filter(ServiceConfig::getRequiresDocker)
                .map(ServiceConfig::getContainerName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .toList();
        
        // STEP 2: Start ALL Docker containers for this phase
        if (!containersToStart.isEmpty()) {
            log.info("🐳 Starting {} Docker containers for phase {}: {}", 
                    containersToStart.size(), phase, containersToStart);
            
            for (String containerName : containersToStart) {
                boolean started = serviceManager.getDockerManager().startContainer(containerName);
                if (!started) {
                    log.error("❌ Failed to start Docker container: {}", containerName);
                    return ResponseEntity.ok(ServiceControlResponse.builder()
                            .serviceName("Phase " + phase)
                            .status(ServiceStatus.ERROR)
                            .message(String.format("Failed to start Docker container: %s", containerName))
                            .success(false)
                            .timestamp(LocalDateTime.now())
                            .build());
                }
            }
            
            log.info("✅ All {} Docker containers started for phase {}", containersToStart.size(), phase);
            
            // Wait for containers to be healthy
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // STEP 3: Start Java services
        // NOTE: 3s gap between each service to avoid concurrent @Async tasks all hammering
        // the HikariCP pool at the same time. With @Transactional removed from startService()
        // each save() is short-lived, so 3s is enough buffer.
        log.info("🚀 Starting {} Java services for phase {}", services.size(), phase);
        for (ServiceConfig service : services) {
            if (service.getEnabled()) {
                serviceManager.startService(service.getServiceName());
                try {
                    Thread.sleep(3000); // Wait 3s between services (was 2s)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("Phase " + phase)
                .status(ServiceStatus.STARTING)
                .message(String.format("Started %d Docker containers and %d services in phase %s", 
                        containersToStart.size(), services.size(), phase))
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Stop services by phase (P0, P1, P2, P3, P4)
     * Stops Java services in reverse order, then stops their Docker containers
     */
    @PostMapping("/phase/{phase}/stop")
    public ResponseEntity<ServiceControlResponse> stopPhase(@PathVariable String phase) {
        log.info("🛑 API request to stop phase: {}", phase);

        List<ServiceConfig> services = configRepository.findByPhaseOrderByStartupOrder(phase);

        // Stop in reverse order
        List<ServiceConfig> reversed = new java.util.ArrayList<>(services);
        java.util.Collections.reverse(reversed);

        int stoppedCount = 0;
        for (ServiceConfig service : reversed) {
            if (service.getEnabled()) {
                boolean stopped = serviceManager.stopService(service.getServiceName());
                if (stopped) stoppedCount++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Stop Docker containers for this phase
        List<String> containersToStop = services.stream()
                .filter(ServiceConfig::getEnabled)
                .filter(ServiceConfig::getRequiresDocker)
                .map(ServiceConfig::getContainerName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .toList();

        int dockerStopped = 0;
        for (String containerName : containersToStop) {
            boolean stopped = serviceManager.getDockerManager().stopContainer(containerName);
            if (stopped) dockerStopped++;
        }

        log.info("✅ Phase {} stopped: {} services, {} Docker containers", phase, stoppedCount, dockerStopped);

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("Phase " + phase)
                .status(ServiceStatus.STOPPED)
                .message(String.format("Stopped %d services and %d Docker containers in phase %s",
                        stoppedCount, dockerStopped, phase))
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Get phase status summary
     */
    @GetMapping("/phase/{phase}/status")
    public ResponseEntity<Map<String, Object>> getPhaseStatus(@PathVariable String phase) {
        List<ServiceConfig> services = configRepository.findByPhaseOrderByStartupOrder(phase);

        long running = services.stream()
                .filter(s -> serviceManager.getServiceStatus(s.getServiceName()) == ServiceStatus.RUNNING)
                .count();
        long stopped = services.stream()
                .filter(s -> serviceManager.getServiceStatus(s.getServiceName()) == ServiceStatus.STOPPED)
                .count();
        long error = services.stream()
                .filter(s -> serviceManager.getServiceStatus(s.getServiceName()) == ServiceStatus.ERROR)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("phase", phase);
        response.put("total", services.size());
        response.put("running", running);
        response.put("stopped", stopped);
        response.put("error", error);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Start tất cả MySQL containers và grant tpnam/121831 permissions.
     * Gọi khi: lần đầu setup, sau docker compose down/up, hoặc gặp "Access denied for tpnam".
     */
    @PostMapping("/databases/start")
    public ResponseEntity<ServiceControlResponse> startAllDatabases() {
        log.info("🐳 API request to start ALL database containers");

        CompletableFuture.runAsync(dockerManager::startAllDatabases);

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("ALL-DATABASES")
                .status(ServiceStatus.STARTING)
                .message("Starting all MySQL containers and granting tpnam/121831 permissions (async, ~30s)")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Grant tpnam/121831 permissions trên tất cả MySQL containers đang chạy.
     * Dùng khi containers đã up nhưng user tpnam chưa có quyền.
     */
    @PostMapping("/databases/grant-permissions")
    public ResponseEntity<ServiceControlResponse> grantAllDatabasePermissions() {
        log.info("🔐 API request to grant tpnam permissions on all DB containers");

        CompletableFuture.runAsync(dockerManager::grantAllDatabasePermissions);

        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("ALL-DATABASES")
                .status(ServiceStatus.STARTING)
                .message("Granting tpnam/121831 permissions on all running MySQL containers (async, ~20s)")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ==========================================
    // CDS (Class Data Sharing) endpoints
    // ==========================================

    /**
     * GET /api/services/cds/status
     * Show CDS archive status for every service:
     * whether archive exists, size (KB), whether it's stale (JAR newer than archive).
     */
    @GetMapping("/cds/status")
    public ResponseEntity<Map<String, Object>> getCdsStatus() {
        var rows = serviceManager.getCdsStatus();
        long total    = rows.size();
        long hasArchive = rows.stream().filter(r -> Boolean.TRUE.equals(r.get("archiveExists"))).count();
        long stale    = rows.stream().filter(r -> Boolean.TRUE.equals(r.get("stale"))).count();
        long noJar    = rows.stream().filter(r -> Boolean.FALSE.equals(r.get("jarExists"))).count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", Map.of(
                "total", total,
                "archiveReady", hasArchive,
                "stale", stale,
                "noJar", noJar,
                "missing", total - hasArchive
        ));
        response.put("services", rows);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/services/cds/warm-all
     * Create CDS archives for all services that have a JAR but no (or stale) archive.
     * Runs async — returns immediately. Check /cds/status to monitor progress.
     *
     * IMPORTANT: DB containers and Redis must be running before calling this,
     * because the training run boots the full Spring ApplicationContext.
     */
    @PostMapping("/cds/warm-all")
    public ResponseEntity<ServiceControlResponse> warmAllCds() {
        log.info("🏗️ API request: warm CDS archives for all services");
        serviceManager.warmAllCdsArchives();
        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName("ALL")
                .status(ServiceStatus.STARTING)
                .message("CDS warm-up started async. Monitor progress at GET /api/services/cds/status")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * DELETE /api/services/{serviceName}/cds
     * Delete the CDS archive for one service (use after mvn package rebuilds the JAR).
     * The next start of that service will run a new training run automatically.
     */
    @DeleteMapping("/{serviceName}/cds")
    public ResponseEntity<ServiceControlResponse> deleteCdsArchive(@PathVariable String serviceName) {
        log.info("🗑️ API request: delete CDS archive for {}", serviceName);
        boolean deleted = serviceManager.deleteCdsArchive(serviceName);
        return ResponseEntity.ok(ServiceControlResponse.builder()
                .serviceName(serviceName)
                .status(ServiceStatus.STOPPED)
                .message(deleted ? "CDS archive deleted. Next start will recreate it." : "No archive found.")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Update service configuration
     */
    @PutMapping("/{serviceName}")
    public ResponseEntity<ServiceInfo> updateService(
            @PathVariable String serviceName,
            @RequestBody ServiceConfig updates) {

        return configRepository.findByServiceName(serviceName)
                .map(config -> {
                    // Update allowed fields
                    if (updates.getDisplayName() != null) {
                        config.setDisplayName(updates.getDisplayName());
                    }
                    if (updates.getAutoStart() != null) {
                        config.setAutoStart(updates.getAutoStart());
                    }
                    if (updates.getEnabled() != null) {
                        config.setEnabled(updates.getEnabled());
                    }
                    if (updates.getJvmArgs() != null) {
                        config.setJvmArgs(updates.getJvmArgs());
                    }
                    if (updates.getDescription() != null) {
                        config.setDescription(updates.getDescription());
                    }

                    ServiceConfig saved = configRepository.save(config);
                    return ResponseEntity.ok(toServiceInfo(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convert entity to DTO
     */
    private ServiceInfo toServiceInfo(ServiceConfig config) {
        // Get real-time status
        ServiceStatus currentStatus = serviceManager.getServiceStatus(config.getServiceName());

        // Get Docker status if applicable
        String dockerStatus = null;
        if (config.getRequiresDocker() && config.getContainerName() != null) {
            dockerStatus = dockerManager.getContainerStatus(config.getContainerName());
        }

        return ServiceInfo.builder()
                .id(config.getId())
                .serviceName(config.getServiceName())
                .displayName(config.getDisplayName())
                .port(config.getPort())
                .phase(config.getPhase())
                .startupOrder(config.getStartupOrder())
                .status(currentStatus)
                .enabled(config.getEnabled())
                .autoStart(config.getAutoStart())
                .processId(config.getProcessId())
                .description(config.getDescription())
                .healthCheckUrl(config.getHealthCheckUrl())
                .containerName(config.getContainerName())
                .requiresDocker(config.getRequiresDocker())
                .dockerStatus(dockerStatus)
                .build();
    }
}
