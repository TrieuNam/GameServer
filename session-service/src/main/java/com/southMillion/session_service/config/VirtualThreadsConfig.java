package com.southMillion.session_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadsConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService vtExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public Scheduler blockingScheduler(ExecutorService vtExecutor) {
        return Schedulers.fromExecutorService(vtExecutor);
    }
}