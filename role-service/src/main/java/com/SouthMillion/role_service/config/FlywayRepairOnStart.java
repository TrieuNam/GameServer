package com.SouthMillion.role_service.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairOnStart {
    @Bean
    FlywayMigrationStrategy repairThenMigrate() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                flyway.repair();   // xóa bản ghi failed, đồng bộ checksum
                flyway.migrate();  // chạy migrate
            }
        };
    }
}