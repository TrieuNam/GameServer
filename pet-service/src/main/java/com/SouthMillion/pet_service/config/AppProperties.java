package com.SouthMillion.pet_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.config")
public class AppProperties {
    private String serviceName;
    private String pet_auto;
    private String pet_cloth_auto;
    private String pet_cloth_game_auto;
}