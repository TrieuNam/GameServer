package com.SouthMillion.config_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowCredentials(false);
        cors.setAllowedOrigins(List.of(
                "http://localhost:7456",
                "http://127.0.0.1:7456"
        ));
        cors.setAllowedMethods(List.of("GET", "HEAD", "OPTIONS")); // không cho POST từ web vào /internal/**
        cors.addAllowedHeader(org.springframework.web.cors.CorsConfiguration.ALL);
        cors.setExposedHeaders(List.of("ETag", "Cache-Control", "Last-Modified", "Content-Type"));
        cors.setMaxAge(Duration.ofHours(1));

        // Chỉ bật CORS cho /api/**. /internal/** mặc định không CORS (an toàn hơn)
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return new CorsFilter(source);
    }
}