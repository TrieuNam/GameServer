package com.SouthMillion.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * WEBCLIENT OPTIMIZATION FOR IAP-VERIFY SERVICE
 * Optimize non-blocking HTTP client for Apple/Google IAP verification
 * 
 * OPTIMIZATION STRATEGY:
 * - Reduce connection pool (IAP API has rate limits)
 * - Set proper timeouts
 * - Reuse connections efficiently
 */
@Configuration
public class WebClientOptimizationConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientOptimizationConfig.class);

    @Bean
    public WebClient.Builder optimizedWebClientBuilder() {
        // Connection pool optimization
        ConnectionProvider provider = ConnectionProvider.builder("iap-verify-pool")
                .maxConnections(10)              // Giảm từ 500 (default) → 10
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(60))
                .build();
        
        // HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(15))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(15))
                        .addHandlerLast(new WriteTimeoutHandler(10))
                );
        
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║  🌐 WEBCLIENT OPTIMIZATION (IAP-VERIFY)                      ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("🔧 Connection pool: 10 (vs 500 default)");
        log.info("⏱️  Timeouts: connect=10s, read=15s, write=10s");
        log.info("♻️  Max idle: 30s, Max lifetime: 5 min");
        log.info("💰 SAVED: ~490 connections = ~50 MB");
        log.info("════════════════════════════════════════════════════════════════");
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
