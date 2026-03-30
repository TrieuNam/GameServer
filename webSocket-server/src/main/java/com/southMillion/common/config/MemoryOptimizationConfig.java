package com.SouthMillion.common.config;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
/**
 * ULTRA-LOW MEMORY OPTIMIZATION - WebFlux/Netty variant
 * Tomcat beans are intentionally absent: this service uses Netty (WebFlux), not Tomcat.
 * Importing Tomcat classes (ProtocolHandler, Connector) would cause compile errors
 * because tomcat-embed-core is not on the classpath for WebFlux services.
 *
 * TIER 1 - CRITICAL (eureka, gateway, config): 128-256 MB
 * TIER 2 - MINIMAL (business services): 48-96 MB
 * TIER 3 - ULTRA-LOW (background services): 32-64 MB
 */
@Configuration
public class MemoryOptimizationConfig {
    private static final Logger log = LoggerFactory.getLogger(MemoryOptimizationConfig.class);
    @Value("${spring.application.name:unknown}")
    private String appName;
    private ServiceTier serviceTier;
    @PostConstruct
    public void init() {
        this.serviceTier = detectServiceTier();
        logMemorySettings();
    }
    private ServiceTier detectServiceTier() {
        if (appName.contains("eureka") || appName.contains("gateway") || appName.contains("config")) {
            log.info("🔥 Detected CRITICAL tier service: {}", appName);
            return ServiceTier.CRITICAL;
        }
        if (appName.contains("analytics") || appName.contains("scheduler") ||
            appName.contains("file") || appName.contains("localization") ||
            appName.contains("moderation")) {
            log.info("⚡ Detected ULTRA-LOW tier service: {}", appName);
            return ServiceTier.ULTRA_LOW;
        }
        log.info("🎯 Detected MINIMAL tier service: {}", appName);
        return ServiceTier.MINIMAL;
    }
    // NOTE: No Tomcat beans here — this service uses Netty (WebFlux).
    // Virtual threads for Netty are configured via reactor.netty or application.yml.
    private void logMemorySettings() {
        MemoryMXBean memoryBean  = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage    = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        String heapMaxStr    = heapUsage.getMax()    > 0 ? (heapUsage.getMax()    / 1024 / 1024) + " MB" : "N/A";
        String nonHeapMaxStr = nonHeapUsage.getMax() > 0 ? (nonHeapUsage.getMax() / 1024 / 1024) + " MB" : "N/A";
        log.info("════════════════════════════════════════════════");
        log.info("🚀 ULTRA-LOW MEMORY MODE (WebFlux/Netty)");
        log.info("════════════════════════════════════════════════");
        log.info("📊 Service Tier: {} ({})", serviceTier, appName);
        log.info("💾 Heap Memory:");
        log.info("   Initial: {} MB", heapUsage.getInit() / 1024 / 1024);
        log.info("   Max:     {}", heapMaxStr);
        log.info("💾 Non-Heap (Metaspace):");
        log.info("   Initial: {} MB", nonHeapUsage.getInit() / 1024 / 1024);
        log.info("   Max:     {}", nonHeapMaxStr);
        log.info("   Server:  Netty (WebFlux) — no Tomcat thread pool");
        log.info("════════════════════════════════════════════════");
    }
    public enum ServiceTier {
        CRITICAL(200, 50, 500),
        MINIMAL(150, 20, 300),
        ULTRA_LOW(100, 10, 200);
        private final int virtualMaxThreads;
        private final int virtualMinSpareThreads;
        private final int maxConnections;
        ServiceTier(int virtualMaxThreads, int virtualMinSpareThreads, int maxConnections) {
            this.virtualMaxThreads      = virtualMaxThreads;
            this.virtualMinSpareThreads = virtualMinSpareThreads;
            this.maxConnections         = maxConnections;
        }
        public int getVirtualMaxThreads()      { return virtualMaxThreads; }
        public int getVirtualMinSpareThreads() { return virtualMinSpareThreads; }
        public int getMaxConnections()         { return maxConnections; }
    }
}