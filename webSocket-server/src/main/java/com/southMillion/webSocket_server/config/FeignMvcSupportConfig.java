package com.SouthMillion.webSocket_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class FeignMvcSupportConfig {
    @Bean
    public HttpMessageConverters httpMessageConverters(ObjectMapper om) {
        return new HttpMessageConverters(
                new MappingJackson2HttpMessageConverter(om)
        );
    }
}