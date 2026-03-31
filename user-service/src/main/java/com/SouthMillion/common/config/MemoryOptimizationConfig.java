package com.SouthMillion.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;

/**
 * ULTRA-LOW MEMORY OPTIMIZATION
 * Auto-detects service tier and applies runtime optimizations
 *
 * TIER 1 - CRITICAL (eureka, gateway, config): 128-256 MB
 * TIER 2 - MINIMAL (business services): 48-96 MB
 * TIER 3 - ULTRA-LOW (background services): 32-64 MB
 *
 * Tomcat beans chỉ áp dụng cho Servlet stack (không áp dụng cho WebFlux/Netty).
 */
@Configuration
public class MemoryOptimizationConfig {

    private static final Logger log = LoggerFactory.getLogger(MemoryOptimizationConfig.class);

    /**
     * Fix: dùng @Value thay vì System.getProperty().
     * System.getProperty() không đọc được application.yml — luôn trả về ""
     * khiến mọi service đều vào MINIMAL tier dù là eureka/gateway/config.
     */
    @Value("${spring.application.name:unknown}")
    private String appName;

    private ServiceTier serviceTier;

    /**
     * Fix: chuyển logic khởi tạo vào @PostConstruct thay vì constructor.
     * Constructor chạy TRƯỚC khi Spring inject @Value → appName luôn null trong constructor.
     */
    @PostConstruct
    public void init() {
        this.serviceTier = detectServiceTier();
        logMemorySettings();
    }

    /**
     * Auto-detect service tier từ application name (đã được Spring inject đúng)
     */
    private ServiceTier detectServiceTier() {
        // CRITICAL tier - Infrastructure services
        if (appName.contains("eureka") || appName.contains("gateway") || appName.contains("config")) {
            log.info("🔥 Detected CRITICAL tier service: {}", appName);
            return ServiceTier.CRITICAL;
        }
        // ULTRA-LOW tier - Background services
        if (appName.contains("analytics") || appName.contains("scheduler") ||
            appName.contains("file") || appName.contains("localization") ||
            appName.contains("moderation")) {
            log.info("⚡ Detected ULTRA-LOW tier service: {}", appName);
            return ServiceTier.ULTRA_LOW;
        }
        // Default: MINIMAL tier
        log.info("🎯 Detected MINIMAL tier service: {}", appName);
        return ServiceTier.MINIMAL;
    }

    /**
     * Bật Virtual Threads cho Tomcat (Java 21+).
     *
     * Fix: @ConditionalOnWebApplication(SERVLET) — bean này chỉ tạo khi service
     * dùng Tomcat (Servlet stack). Với gateway/webSocket-server dùng Netty (WebFlux),
     * bean sẽ bị BỎ QUA, tránh lỗi NoSuchBeanDefinitionException.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            log.info("🚀 ENABLING VIRTUAL THREADS for Tomcat (Java 21)");
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * Tinh chỉnh Tomcat theo service tier.
     *
     * Fix: @ConditionalOnWebApplication(SERVLET) — tương tự bean trên.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            int maxThreads      = serviceTier.getVirtualMaxThreads();
            int minSpareThreads = serviceTier.getVirtualMinSpareThreads();
            int maxConnections  = serviceTier.getMaxConnections();

            connector.setProperty("maxThreads",       String.valueOf(maxThreads));
            connector.setProperty("minSpareThreads",  String.valueOf(minSpareThreads));
            connector.setProperty("maxConnections",   String.valueOf(maxConnections));
            connector.setProperty("acceptCount",      "100");
            connector.setProperty("connectionTimeout","20000");

            log.info("🔧 Tomcat optimized with VIRTUAL THREADS");
            log.info("   Threads: {}/{} (Virtual - very lightweight!)", minSpareThreads, maxThreads);
            log.info("   Connections: {}", maxConnections);
            log.info("   Memory per thread: ~1KB (vs 1MB for platform threads)");
        });
    }

    /**
     * Fix: getMax() trả về -1 khi JVM không giới hạn (thường gặp với Non-Heap/Metaspace)
     * → hiển thị "N/A" thay vì "-1 MB"
     */
    private void logMemorySettings() {
        MemoryMXBean memoryBean  = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage    = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        String heapMaxStr    = heapUsage.getMax()    > 0 ? (heapUsage.getMax()    / 1024 / 1024) + " MB" : "N/A";
        String nonHeapMaxStr = nonHeapUsage.getMax() > 0 ? (nonHeapUsage.getMax() / 1024 / 1024) + " MB" : "N/A";

        log.info("════════════════════════════════════════════════");
        log.info("🚀 ULTRA-LOW MEMORY MODE + VIRTUAL THREADS");
        log.info("════════════════════════════════════════════════");
        log.info("📊 Service Tier: {} ({})", serviceTier, appName);
        log.info("💾 Heap Memory:");
        log.info("   Initial: {} MB", heapUsage.getInit() / 1024 / 1024);
        log.info("   Max:     {}", heapMaxStr);
        log.info("💾 Non-Heap (Metaspace):");
        log.info("   Initial: {} MB", nonHeapUsage.getInit() / 1024 / 1024);
        log.info("   Max:     {}", nonHeapMaxStr);
        log.info("🔧 Virtual Threads: {}-{} (1KB each!)", serviceTier.getVirtualMinSpareThreads(), serviceTier.getVirtualMaxThreads());
        log.info("🔧 Max Connections: {}", serviceTier.getMaxConnections());
        log.info("════════════════════════════════════════════════");
    }

    /**
     * Service tier enum with memory profiles
     * VIRTUAL THREADS MODE: Can use 10x more threads with same memory!
     */
    public enum ServiceTier {
        CRITICAL(200, 50, 500),    // Infrastructure: eureka, gateway, config
        MINIMAL(150, 20, 300),     // Business services (default)
        ULTRA_LOW(100, 10, 200);   // Background services

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
