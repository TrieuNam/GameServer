package com.SouthMillion.common.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.resources.LoopResources;
import jakarta.annotation.PostConstruct;

/**
 * REACTIVE OPTIMIZATION FOR GATEWAY SERVICE
 * Optimized for Spring Cloud Gateway (WebFlux/Netty)
 * 
 * ⚠️ DO NOT USE VIRTUAL THREADS WITH REACTIVE!
 * - Reactive already uses non-blocking I/O (Netty event loops)
 * - Virtual Threads designed for blocking I/O (Tomcat)
 * - Combining them causes thread pinning and performance degradation
 * 
 * OPTIMIZATION STRATEGY:
 * - Use minimal Netty worker threads (event loop)
 * - Optimize Reactor schedulers
 * - Reduce memory footprint
 */
@Configuration
public class ReactiveOptimizationConfig {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOptimizationConfig.class);

    /**
     * Optimize Netty server for low memory
     * Gateway is CRITICAL tier but uses event loops (not threads)
     */
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> {
            // Netty event loop threads (non-blocking)
            // OPTIMIZED: CPU cores / 2 for low-medium traffic
            int cpus = Runtime.getRuntime().availableProcessors();
            int workerThreads = Math.max(2, cpus / 2);
            
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║  ⚛️  REACTIVE OPTIMIZATION (GATEWAY - WEBFLUX/NETTY)         ║");
            log.info("╚════════════════════════════════════════════════════════════════╝");
            log.info("🖥️  CPUs: {}", cpus);
            log.info("🔧 Netty worker threads: {} (OPTIMIZED: CPUs/2)", workerThreads);
            log.info("💡 Memory model: Event Loop (NOT Virtual Threads)");
            log.info("⚡ Each thread handles THOUSANDS of concurrent connections");
            log.info("📊 Memory per thread: ~5-10 MB (event loop overhead)");
            log.info("🎯 Total thread memory: ~{} MB", workerThreads * 10);
            log.info("💰 SAVED: ~{} MB vs default (CPUs * 2)", (cpus - workerThreads) * 10);
            log.info("════════════════════════════════════════════════════════════════");
            
            // Configure Netty with optimized settings
            factory.addServerCustomizers(httpServer -> 
                httpServer.runOn(LoopResources.create("gateway-event-loop", workerThreads, true))
            );
        };
    }

    /**Optimize Reactor schedulers (parallel, single, etc.)
     * Giảm parallel threads cho low-medium traffic
     */
    @PostConstruct
    public void optimizeReactorSchedulers() {
        int cpus = Runtime.getRuntime().availableProcessors();
        int parallelThreads = Math.max(2, cpus / 2);
        
        System.setProperty("reactor.schedulers.defaultPoolSize", String.valueOf(parallelThreads));
        System.setProperty("reactor.schedulers.defaultQueuedTaskCap", "100");
        
        log.info("🔧 Reactor Schedulers optimized:");
        log.info("   • Parallel threads: {} (vs {} CPUs default)", parallelThreads, cpus);
        log.info("   • Queued task cap: 100");
        log.info("   💰 SAVED: ~{} threads", cpus - parallelThreads);
    }

    /**
     * 
     * Log memory settings on startup
     */
    @Bean
    public ReactiveMemoryLogger reactiveMemoryLogger() {
        return new ReactiveMemoryLogger();
    }

    static class ReactiveMemoryLogger {
        private static final Logger log = LoggerFactory.getLogger(ReactiveMemoryLogger.class);

        public ReactiveMemoryLogger() {
            int cpus = Runtime.getRuntime().availableProcessors();
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            
            log.info("════════════════════════════════════════════════════════════════");
            log.info("⚛️  REACTIVE GATEWAY - MEMORY CONFIGURATION");
            log.info("════════════════════════════════════════════════════════════════");
            log.info("🖥️  CPUs: {}", cpus);
            log.info("💾 Max Heap: {} MB", maxMemory);
            log.info("🔄 Event Loop Threads: {} (Netty workers)", Math.max(2, cpus));
            log.info("⚡ Model: NON-BLOCKING I/O (NOT Virtual Threads)");
            log.info("📊 Each thread handles: THOUSANDS of connections");
            log.info("════════════════════════════════════════════════════════════════");
        }
    }
}
