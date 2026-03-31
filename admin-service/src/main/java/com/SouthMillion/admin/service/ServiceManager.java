package com.SouthMillion.admin.service;

import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import com.SouthMillion.admin.repository.ServiceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service Process Manager
 * Manages lifecycle of GameServer microservices (start/stop/restart)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceManager {

    private final ServiceConfigRepository configRepository;
    private final DockerManager dockerManager;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gameserver.base.path:D:/project/serverGame/GameServer}")
    private String baseGameServerPath;

    /**
     * Map to track running processes: serviceName -> Process
     */
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    /**
     * Map to track log outputs: serviceName -> List<String>
     */
    private final Map<String, List<String>> serviceLogs = new ConcurrentHashMap<>();

    /**
     * Start a service
     * NOTE: @Transactional intentionally removed — this is a long-running async operation
     * (can take 60+ seconds waiting for health check). Holding a DB connection open that long
     * exhausts HikariCP pool when multiple services start concurrently (P1 = 13 services).
     * Each repository.save() call creates its own short transaction via the repository layer.
     */
    @Async
    public CompletableFuture<Boolean> startService(String serviceName) {
        log.info("🚀 Starting service: {}", serviceName);

        Optional<ServiceConfig> configOpt = configRepository.findByServiceName(serviceName);
        if (configOpt.isEmpty()) {
            log.error("❌ Service not found: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }

        ServiceConfig config = configOpt.get();
        if (!config.getEnabled()) {
            log.warn("⚠️ Service is disabled: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }

        // Check if already running
        if (runningProcesses.containsKey(serviceName) && isProcessAlive(serviceName)) {
            log.warn("⚠️ Service already running: {}", serviceName);
            return CompletableFuture.completedFuture(true);
        }

        try {
            // Update status to STARTING
            config.setStatus(ServiceStatus.STARTING);
            config.setLastStarted(LocalDateTime.now());
            configRepository.save(config);

            // SPECIAL CASE: Eureka starts core infrastructure (Redis, Kafka) if not already running
            // NOTE: Eureka itself doesn't need Redis/Kafka, but other P0 services do
            if ("eureka-server".equals(serviceName)) {
                log.info("🚀 Eureka detected - Starting P0 infrastructure (Redis, Kafka)...");
                boolean infraStarted = dockerManager.startAllInfrastructure();
                if (!infraStarted) {
                    log.warn("⚠️ Failed to start infrastructure for Eureka - continuing anyway (Eureka doesn't require them)");
                    // Don't fail - Eureka can run without Redis/Kafka
                }
                log.info("✅ P0 infrastructure check completed!");
            }
            // NORMAL CASE: Start Docker container if required (for individual service start)
            // Note: When starting via phase, Docker containers are started at phase level
            else if (config.getRequiresDocker() && config.getContainerName() != null) {
                log.info("🐳 Checking Docker container: {}", config.getContainerName());
                
                // Check if container is already running
                if (!dockerManager.isContainerRunning(config.getContainerName())) {
                    log.info("🐳 Starting Docker container: {}", config.getContainerName());
                    boolean dockerStarted = dockerManager.startContainer(config.getContainerName());
                    if (!dockerStarted) {
                        log.error("❌ Failed to start Docker container for: {}", serviceName);
                        config.setStatus(ServiceStatus.ERROR);
                        configRepository.save(config);
                        return CompletableFuture.completedFuture(false);
                    }
                    log.info("✅ Docker container started: {}", config.getContainerName());
                } else {
                    log.info("✅ Docker container already running: {}", config.getContainerName());
                }
            }

            // Build command
            List<String> command = buildStartCommand(config);
            log.info("🚀 Command: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Set working directory - resolve relative paths to absolute
            String workingDir = config.getWorkingDirectory();
            if (workingDir != null) {
                // If relative path, resolve from base path
                File workDir = new File(workingDir);
                if (!workDir.isAbsolute()) {
                    workDir = new File(baseGameServerPath, workingDir.replace("..", "."));
                }
                processBuilder.directory(workDir);
                log.info("📁 Working directory: {}", workDir.getAbsolutePath());
            } else {
                // Fallback to service folder
                File workDir = new File(baseGameServerPath, serviceName);
                processBuilder.directory(workDir);
                log.info("📁 Working directory (default): {}", workDir.getAbsolutePath());
            }

            // Redirect error stream to output
            processBuilder.redirectErrorStream(true);

            // Start process
            log.info("🔥 Starting process...");
            Process process = processBuilder.start();
            
            // Check if process started successfully
            if (!process.isAlive()) {
                log.error("❌ Process failed to start immediately!");
                config.setStatus(ServiceStatus.ERROR);
                configRepository.save(config);
                return CompletableFuture.completedFuture(false);
            }
            
            runningProcesses.put(serviceName, process);

            // Get PID
            long pid = process.pid();
            config.setProcessId(pid);
            log.info("✅ Process started with PID: {}", pid);

            // Start log reader thread
            startLogReader(serviceName, process);
            
            // Give it a moment to start logging
            Thread.sleep(2000);

            // Wait for service to be healthy
            boolean healthy = waitForHealthCheck(config, 60);

            if (healthy) {
                config.setStatus(ServiceStatus.RUNNING);
                log.info("✅ Service started successfully: {} (Port: {})", serviceName, config.getPort());
            } else {
                config.setStatus(ServiceStatus.ERROR);
                log.error("❌ Service failed health check: {}", serviceName);
            }

            configRepository.save(config);
            return CompletableFuture.completedFuture(healthy);

        } catch (Exception e) {
            log.error("❌ Failed to start service: {}", serviceName, e);
            config.setStatus(ServiceStatus.ERROR);
            configRepository.save(config);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Stop a service
     */
    @Transactional
    public boolean stopService(String serviceName) {
        log.info("🛑 Stopping service: {}", serviceName);

        Optional<ServiceConfig> configOpt = configRepository.findByServiceName(serviceName);
        if (configOpt.isEmpty()) {
            log.error("❌ Service not found: {}", serviceName);
            return false;
        }

        ServiceConfig config = configOpt.get();
        Process process = runningProcesses.get(serviceName);

        if (process == null || !process.isAlive()) {
            log.warn("⚠️ Service is not running: {}", serviceName);
            config.setStatus(ServiceStatus.STOPPED);
            configRepository.save(config);
            return true;
        }

        try {
            // Update status
            config.setStatus(ServiceStatus.STOPPING);
            config.setLastStopped(LocalDateTime.now());
            configRepository.save(config);

            // Try graceful shutdown first (via actuator)
            try {
                String shutdownUrl = "http://localhost:" + config.getPort() + "/actuator/shutdown";
                restTemplate.postForObject(shutdownUrl, null, String.class);
                log.info("✅ Sent graceful shutdown signal to: {}", serviceName);
            } catch (Exception e) {
                log.warn("⚠️ Graceful shutdown failed, using force kill");
            }

            // Wait for graceful shutdown
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);

            if (!exited) {
                // Force kill if still alive
                log.warn("⚠️ Force killing service: {}", serviceName);
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }

            // Cleanup
            runningProcesses.remove(serviceName);
            serviceLogs.remove(serviceName);

            config.setStatus(ServiceStatus.STOPPED);
            config.setProcessId(null);
            configRepository.save(config);

            log.info("✅ Service stopped: {}", serviceName);

            // Stop Docker container if required
            if (config.getRequiresDocker() && config.getContainerName() != null) {
                log.info("🐳 Stopping Docker container: {}", config.getContainerName());
                boolean dockerStopped = dockerManager.stopContainer(config.getContainerName());
                if (!dockerStopped) {
                    log.warn("⚠️ Failed to stop Docker container: {}", config.getContainerName());
                } else {
                    log.info("✅ Docker container stopped: {}", config.getContainerName());
                }
            }

            return true;

        } catch (Exception e) {
            log.error("❌ Failed to stop service: {}", serviceName, e);
            return false;
        }
    }

    /**
     * Restart a service
     */
    public CompletableFuture<Boolean> restartService(String serviceName) {
        log.info("🔄 Restarting service: {}", serviceName);
        stopService(serviceName);
        try {
            Thread.sleep(2000); // Wait 2 seconds between stop and start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return startService(serviceName);
    }

    /**
     * Get service status
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        Optional<ServiceConfig> config = configRepository.findByServiceName(serviceName);
        if (config.isEmpty()) {
            return ServiceStatus.UNKNOWN;
        }

        // Check if process is actually running
        if (runningProcesses.containsKey(serviceName) && isProcessAlive(serviceName)) {
            return ServiceStatus.RUNNING;
        } else if (config.get().getStatus() == ServiceStatus.RUNNING) {
            // Update status if process died
            config.get().setStatus(ServiceStatus.STOPPED);
            configRepository.save(config.get());
            return ServiceStatus.STOPPED;
        }

        return config.get().getStatus();
    }

    /**
     * Get service logs
     */
    public List<String> getServiceLogs(String serviceName, int lines) {
        List<String> logs = serviceLogs.getOrDefault(serviceName, new ArrayList<>());
        int size = logs.size();
        if (lines <= 0 || lines >= size) {
            return new ArrayList<>(logs);
        }
        return new ArrayList<>(logs.subList(size - lines, size));
    }

    /**
     * Start all services by priority order
     */
    @Async
    public CompletableFuture<Map<String, Boolean>> startAllServices() {
        log.info("🚀 Starting all services in priority order...");

        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        Map<String, Boolean> results = new LinkedHashMap<>();

        for (ServiceConfig service : services) {
            if (!service.getEnabled()) {
                continue;
            }

            log.info("📦 Starting: {} (Order: {})", service.getServiceName(), service.getStartupOrder());
            CompletableFuture<Boolean> result = startService(service.getServiceName());

            try {
                Boolean success = result.get(120, TimeUnit.SECONDS); // 2 minutes timeout per service
                results.put(service.getServiceName(), success);

                if (success) {
                    log.info("✅ Started: {}", service.getServiceName());
                } else {
                    log.error("❌ Failed: {}", service.getServiceName());
                }

                // Wait between services
                Thread.sleep(3000);

            } catch (Exception e) {
                log.error("❌ Error starting: {}", service.getServiceName(), e);
                results.put(service.getServiceName(), false);
            }
        }

        log.info("✅ All services startup complete!");
        return CompletableFuture.completedFuture(results);
    }

    /**
     * Stop all services (reverse order)
     */
    public Map<String, Boolean> stopAllServices() {
        log.info("🛑 Stopping all services...");

        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        Collections.reverse(services); // Stop in reverse order
        Map<String, Boolean> results = new LinkedHashMap<>();

        for (ServiceConfig service : services) {
            log.info("🛑 Stopping: {}", service.getServiceName());
            boolean success = stopService(service.getServiceName());
            results.put(service.getServiceName(), success);
        }

        log.info("✅ All services stopped!");
        return results;
    }

    /**
     * Build start command for service
     */
    private List<String> buildStartCommand(ServiceConfig config) {
        List<String> command = new ArrayList<>();
        command.add("java");

        // Add Thin Launcher support - CRITICAL for reduced JAR sizes
        command.add("-Dthin.root=../repository");
        log.info("🎯 Thin Launcher enabled: dependencies will load from ../repository/");

        // Add JVM args
        if (config.getJvmArgs() != null && !config.getJvmArgs().isEmpty()) {
            command.addAll(Arrays.asList(config.getJvmArgs().split("\\s+")));
        } else {
            // ULTRA-LOW MEMORY MODE - For weak machines running 51 services
            // Trade-off: Slower performance, frequent GC, but can run on 8-16 GB RAM
            String profile = config.getDescription(); // Use description as profile hint
            
            if (profile != null && (profile.contains("gateway") || profile.contains("eureka") || profile.contains("config"))) {
                // CRITICAL TIER: Infrastructure services (128-512 MB) for 50 services on 35GB RAM
                command.add("-Xms128m");
                command.add("-Xmx512m");
                command.add("-XX:+UseG1GC");
                command.add("-XX:MetaspaceSize=96m");
                command.add("-XX:MaxMetaspaceSize=192m");
                command.add("-Xss2m");  // FIXED: Java 21 + Spring Boot 3.5.3 needs 2MB stack
                command.add("-XX:MaxGCPauseMillis=200");
                log.info("🔥 CRITICAL profile: -Xms128m -Xmx512m -Xss2m (target: 250-450 MB RAM)");
            } else if (profile != null && (profile.contains("analytics") || profile.contains("scheduler") || 
                       profile.contains("file") || profile.contains("localization") || profile.contains("moderation"))) {
                // ULTRA-LOW TIER: Background services (128-256 MB) for 50 services on 35GB RAM
                command.add("-Xms64m");
                command.add("-Xmx256m");
                command.add("-XX:+UseG1GC");
                command.add("-XX:MetaspaceSize=64m");
                command.add("-XX:MaxMetaspaceSize=128m");
                command.add("-Xss2m");  // FIXED: Java 21 + Spring Boot 3.5.3 needs 2MB stack
                command.add("-XX:MaxGCPauseMillis=200");
                command.add("-XX:+UseStringDeduplication");
                log.info("⚡ ULTRA-LOW profile: -Xms64m -Xmx256m -Xss2m (target: 120-230 MB RAM)");
            } else {
                // MINIMAL TIER: Most services (128-384 MB) for 50 services on 35GB RAM
                command.add("-Xms128m");
                command.add("-Xmx384m");
                command.add("-XX:+UseG1GC");
                command.add("-XX:MetaspaceSize=96m");
                command.add("-XX:MaxMetaspaceSize=192m");
                command.add("-Xss2m");  // FIXED: Java 21 + Spring Boot 3.5.3 needs 2MB stack
                command.add("-XX:MaxGCPauseMillis=200");
                command.add("-XX:+UseStringDeduplication");
                log.info("🎯 MINIMAL profile: -Xms128m -Xmx384m -Xss2m (target: 180-350 MB RAM)");
            }
        }

        // Add JAR - resolve relative paths and auto-detect actual jar file
        String jarPath = config.getJarPath();
        File jarFile = null;
        
        if (jarPath == null || jarPath.isEmpty()) {
            // Auto-detect jar file in target directory
            File targetDir = new File(baseGameServerPath, config.getServiceName() + "/target");
            log.debug("Looking for JAR in: {}", targetDir.getAbsolutePath());
            
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] jarFiles = targetDir.listFiles((dir, name) -> 
                    name.startsWith(config.getServiceName()) && 
                    name.endsWith(".jar") && 
                    !name.endsWith("-original.jar")
                );
                
                if (jarFiles != null && jarFiles.length > 0) {
                    // Use the first matching jar file
                    jarFile = jarFiles[0];
                    log.info("🔍 Auto-detected JAR: {} (exists: {})", jarFile.getAbsolutePath(), jarFile.exists());
                } else {
                    // Fallback to default pattern - try common patterns
                    log.debug("No JAR found by filter, trying default patterns...");
                    String[] patterns = {
                        config.getServiceName() + "-1.0.0.jar",
                        config.getServiceName() + "-0.0.1-SNAPSHOT.jar"
                    };
                    
                    for (String pattern : patterns) {
                        File candidate = new File(targetDir, pattern);
                        if (candidate.exists()) {
                            jarFile = candidate;
                            log.info("🔍 Found JAR by pattern: {}", jarFile.getName());
                            break;
                        }
                    }
                    
                    if (jarFile == null) {
                        // Use first pattern as fallback (will trigger build if not exists)
                        jarFile = new File(targetDir, patterns[0]);
                        log.warn("⚠️ No JAR found, will use: {}", jarFile.getName());
                    }
                }
            } else {
                // Directory doesn't exist, use default
                log.warn("⚠️ Target directory does not exist: {}", targetDir.getAbsolutePath());
                jarFile = new File(baseGameServerPath, config.getServiceName() + "/target/" + config.getServiceName() + "-1.0.0.jar");
            }
        } else {
            // Use configured jar path
            jarFile = new File(jarPath);
            if (!jarFile.isAbsolute()) {
                // If starts with ../ convert to relative from base path
                if (jarPath.startsWith("../")) {
                    jarFile = new File(baseGameServerPath, jarPath.substring(3));
                } else {
                    jarFile = new File(baseGameServerPath, jarPath);
                }
            }
            log.info("📦 Using configured JAR path: {}", jarFile.getAbsolutePath());
        }
        
        // Verify jar file exists, if not, try to build it
        if (!jarFile.exists()) {
            log.warn("⚠️ JAR file not found: {}", jarFile.getAbsolutePath());
            log.info("🔨 Attempting to build service: {}", config.getServiceName());
            
            try {
                boolean buildSuccess = buildService(config.getServiceName());
                if (!buildSuccess) {
                    log.error("❌ Failed to build service: {}", config.getServiceName());
                    throw new RuntimeException("Failed to build service: " + config.getServiceName());
                }
                
                // After build, re-detect the JAR file that was actually created
                File targetDir = new File(baseGameServerPath, config.getServiceName() + "/target");
                if (targetDir.exists() && targetDir.isDirectory()) {
                    File[] jarFiles = targetDir.listFiles((dir, name) -> 
                        name.startsWith(config.getServiceName()) && 
                        name.endsWith(".jar") && 
                        !name.endsWith("-original.jar")
                    );
                    
                    if (jarFiles != null && jarFiles.length > 0) {
                        jarFile = jarFiles[0];
                        log.info("✅ Service built successfully, JAR found: {}", jarFile.getName());
                    } else {
                        log.error("❌ Build succeeded but no JAR found in: {}", targetDir.getAbsolutePath());
                        throw new RuntimeException("JAR file not found after build: " + config.getServiceName());
                    }
                } else {
                    log.error("❌ Target directory not found: {}", targetDir.getAbsolutePath());
                    throw new RuntimeException("Target directory not found: " + targetDir.getAbsolutePath());
                }
            } catch (Exception e) {
                log.error("❌ Error building service: {}", e.getMessage());
                throw new RuntimeException("Failed to build service: " + config.getServiceName(), e);
            }
        } else {
            log.info("✅ JAR file exists: {}", jarFile.getAbsolutePath());
        }
        
        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        log.info("📦 JAR: {}", jarFile.getAbsolutePath());

        // Add app args
        if (config.getAppArgs() != null && !config.getAppArgs().isEmpty()) {
            command.addAll(Arrays.asList(config.getAppArgs().split("\\s+")));
        }

        return command;
    }

    /**
     * Wait for service health check
     */
    private boolean waitForHealthCheck(ServiceConfig config, int timeoutSeconds) {
        String healthUrl = config.getHealthCheckUrl();
        int attempts = 0;
        int maxAttempts = timeoutSeconds / 5;

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                String response = restTemplate.getForObject(healthUrl, String.class);
                if (response != null && response.contains("UP")) {
                    return true;
                }
            } catch (Exception e) {
                // Service not ready yet
            }
            attempts++;
        }

        return false;
    }

    /**
     * Check if process is alive
     */
    private boolean isProcessAlive(String serviceName) {
        Process process = runningProcesses.get(serviceName);
        return process != null && process.isAlive();
    }

    /**
     * Start log reader thread
     */
    private void startLogReader(String serviceName, Process process) {
        serviceLogs.putIfAbsent(serviceName, Collections.synchronizedList(new ArrayList<>()));

        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                List<String> logs = serviceLogs.get(serviceName);

                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                    // Keep only last 1000 lines
                    if (logs.size() > 1000) {
                        logs.remove(0);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading logs for: {}", serviceName, e);
            }
        });

        logThread.setDaemon(true);
        logThread.setName("LogReader-" + serviceName);
        logThread.start();
    }
    
    /**
     * Build service using Maven
     */
    private boolean buildService(String serviceName) {
        try {
            File serviceDir = new File(baseGameServerPath, serviceName);
            if (!serviceDir.exists()) {
                log.error("❌ Service directory not found: {}", serviceDir.getAbsolutePath());
                return false;
            }
            
            log.info("🔨 Building service in: {}", serviceDir.getAbsolutePath());
            
            // Build Maven command based on OS
            List<String> command = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows: use mvn.cmd
                command.add("cmd");
                command.add("/c");
                command.add("mvn");
            } else {
                // Unix-like: use mvn directly
                command.add("mvn");
            }
            
            command.add("clean");
            command.add("package");
            command.add("-DskipTests");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(serviceDir);
            processBuilder.redirectErrorStream(true);
            
            log.info("🔨 Running: {}", String.join(" ", command));
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Maven: {}", line);
                    if (line.contains("BUILD SUCCESS")) {
                        log.info("✅ Maven build successful");
                    } else if (line.contains("BUILD FAILURE")) {
                        log.error("❌ Maven build failed");
                    }
                }
            }
            
            // Wait for process to complete (max 5 minutes)
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                log.error("❌ Maven build timeout after 5 minutes");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("✅ Maven build completed successfully for: {}", serviceName);
                return true;
            } else {
                log.error("❌ Maven build failed with exit code: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Error building service: {}", serviceName, e);
            return false;
        }
    }
    
    /**
     * Get DockerManager instance for direct access
     */
    public DockerManager getDockerManager() {
        return dockerManager;
    }
}
