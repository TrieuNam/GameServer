package com.SouthMillion.shop_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper(); // FAIL_ON_UNKNOWN_PROPERTIES đã off trong yaml
    }
}