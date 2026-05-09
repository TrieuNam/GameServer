package com.SouthMillion.activity_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution capability (via @Scheduled).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}