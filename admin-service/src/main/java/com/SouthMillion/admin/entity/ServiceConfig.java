package com.SouthMillion.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Service Configuration Entity
 * Stores metadata for all GameServer microservices
 * Used for service lifecycle management (start/stop/restart)
 */
@Entity
@Table(name = "service_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Service name (must match Spring application name)
     */
    @Column(nullable = false, unique = true)
    private String serviceName;

    /**
     * Display name for UI
     */
    private String displayName;

    /**
     * Service port number
     */
    @Column(nullable = false)
    private Integer port;

    /**
     * JAR file path (relative to GameServer directory)
     */
    private String jarPath;

    /**
     * Working directory for the service
     */
    private String workingDirectory;

    /**
     * Startup priority (1 = first, higher = later)
     * 1: Eureka
     * 2: Gateway, Config
     * 3: Core services (P0)
     * 4: Enhancement services (P1)
     * 5: Feature services (P2)
     */
    @Column(nullable = false)
    private Integer startupOrder;

    /**
     * Phase classification (P0, P1, P2)
     */
    private String phase;

    /**
     * Auto-start on admin service startup
     */
    private Boolean autoStart;

    /**
     * Current status: STOPPED, STARTING, RUNNING, STOPPING, ERROR
     */
    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    /**
     * Process ID (when running)
     */
    private Long processId;

    /**
     * JVM arguments (optional)
     */
    @Column(length = 1000)
    private String jvmArgs;

    /**
     * Application arguments (optional)
     */
    @Column(length = 1000)
    private String appArgs;

    /**
     * Service description
     */
    @Column(length = 500)
    private String description;

    /**
     * Last started timestamp
     */
    private LocalDateTime lastStarted;

    /**
     * Last stopped timestamp
     */
    private LocalDateTime lastStopped;

    /**
     * Enabled/Disabled flag
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Health check endpoint (default: /actuator/health)
     */
    private String healthCheckUrl;

    /**
     * Docker container name (for services with database)
     * Example: mysql_user, mysql_role, redis_main
     */
    private String containerName;

    /**
     * Whether to start Docker container before starting service
     */
    @Column(nullable = false)
    private Boolean requiresDocker;

    /**
     * Created timestamp
     */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated timestamp
     */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
        if (autoStart == null) {
            autoStart = false;
        }
        if (status == null) {
            status = ServiceStatus.STOPPED;
        }
        if (healthCheckUrl == null) {
            healthCheckUrl = "http://localhost:" + port + "/actuator/health";
        }
        if (requiresDocker == null) {
            requiresDocker = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
