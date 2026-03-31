package com.SouthMillion.gateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // .cors(withDefaults())  // BỎ dòng này khi đã dùng CorsWebFilter riêng
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/session-service/api/session/login",
                                "/session-service/api/session/refresh",
                                "/session-service/actuator/**"
                        ).permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }

    // CORS REACTIVE
    @Bean
    public org.springframework.web.cors.reactive.CorsWebFilter corsWebFilter() {
        var cors = new org.springframework.web.cors.CorsConfiguration();
        cors.setAllowCredentials(true);
        cors.setAllowedOriginPatterns(List.of("http://localhost:7456","http://127.0.0.1:7456")); // dùng *patterns* khi allowCredentials=true
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("X-Request-Id","X-Session-Id","X-User-Id","ETag","Cache-Control","Last-Modified","Content-Type"));
        cors.setMaxAge(Duration.ofHours(1));

        var source = new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new org.springframework.web.cors.reactive.CorsWebFilter(source);
    }
}