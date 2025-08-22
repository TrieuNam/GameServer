package com.southMillion.webSocket_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignMvcSupportConfig {
    @Bean
    public org.springframework.boot.autoconfigure.http.HttpMessageConverters httpMessageConverters() {
        return new org.springframework.boot.autoconfigure.http.HttpMessageConverters();
    }
}