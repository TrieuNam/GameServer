package com.SouthMillion.session_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

// CORS được xử lý tại gateway-service — không cần ở đây vì session-service là internal
@Configuration
public class SecurityConfig {

    @Bean
    public Scheduler blockingScheduler() {
        return Schedulers.boundedElastic();
    }
}