package com.SouthMillion.guild_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Async Configuration for Guild Service
 * Uses Virtual Threads (JDK 21+) for async operations
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "guildAsyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        // Sử dụng Virtual Thread executor từ JDK 21
        // Virtual threads tự động scale theo nhu cầu, không cần cấu hình pool size
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("[AsyncConfig] Initialized Virtual Thread executor for async operations");
        return executor;
    }
}
