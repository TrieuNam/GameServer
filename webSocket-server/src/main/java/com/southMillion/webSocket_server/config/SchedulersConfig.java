package com.southMillion.webSocket_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SchedulersConfig {
    /**
     * Scheduler dựa trên Virtual Threads cho các tác vụ blocking như Feign.
     */
    @Bean(destroyMethod = "dispose")
    public reactor.core.scheduler.Scheduler feignVtScheduler() {
        var exec = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        return reactor.core.scheduler.Schedulers.fromExecutorService(exec);
    }
}