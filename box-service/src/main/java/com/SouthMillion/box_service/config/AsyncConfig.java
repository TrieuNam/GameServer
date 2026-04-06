package com.SouthMillion.box_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Cấu hình async executor cho box-service sử dụng Virtual Threads (JDK 21+).
 * Virtual Threads nhẹ hơn và hiệu quả hơn platform threads cho I/O operations,
 * cho phép các thao tác như auto-sell chạy bất đồng bộ để giảm độ trễ phản hồi.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "boxAsyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        // Sử dụng Virtual Thread executor từ JDK 21
        // Virtual threads tự động scale theo nhu cầu, không cần cấu hình pool size
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("[AsyncConfig] Initialized Virtual Thread executor for async operations");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("[AsyncConfig] Uncaught async exception in method={}: {}",
                    method.getName(), throwable.getMessage(), throwable);
        };
    }
}
