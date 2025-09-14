package com.southMillion.session_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;


@Configuration
public class SecurityConfig {



    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowCredentials(false);
        cors.setAllowedOrigins(List.of(
                "http://localhost:7456",
                "http://127.0.0.1:7456"
        ));
        cors.setAllowedMethods(List.of("POST", "GET", "HEAD", "OPTIONS"));
        cors.addAllowedHeader(CorsConfiguration.ALL);
        cors.setExposedHeaders(List.of("ETag", "Cache-Control", "Last-Modified", "Content-Type"));
        cors.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}