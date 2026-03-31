package com.SouthMillion.box_service.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRun {
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.repair();
            } catch (Exception ignore) {
            }
            flyway.migrate();
        };
    }
}
