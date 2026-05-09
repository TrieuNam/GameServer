package com.SouthMillion.activity_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's asynchronous method execution capability (via @Async).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}