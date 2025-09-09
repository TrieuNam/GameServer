package com.SouthMillion.item_service.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class FeignConfig {

    public static class NotModifiedException extends RuntimeException {
        public NotModifiedException() { super("304 Not Modified"); }
    }

    @Bean
    public ErrorDecoder itemErrorDecoder() {
        return (methodKey, response) -> {
            // 304 → trả về exception riêng để Service xử lý như "cache hit"
            if (response.status() == 304) return new NotModifiedException();
            // 404 → cứ để Feign mặc định ném FeignException.NotFound,
            // ta sẽ bắt ở Service và đổi thành ItemNotFoundException
            return decode(methodKey, response);
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}