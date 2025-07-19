package com.SouthMillion.user_service.utils;


import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    String secret;
    @Value("${jwt.expiration}")
    long expiration;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expiration);
    }
}
