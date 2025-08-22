package com.SouthMillion.user_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdGenerator {
    private final AppProperties props;
    public String newUserId() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        String p = props.getId().getPrefix();
        return (p == null || p.isBlank()) ? raw : p + raw;
    }
}