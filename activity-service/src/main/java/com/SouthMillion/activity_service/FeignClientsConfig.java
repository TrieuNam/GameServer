package com.SouthMillion.activity_service;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring Cloud OpenFeign clients for remote REST calls.
 */
@Configuration
@EnableFeignClients
public class FeignClientsConfig {
}