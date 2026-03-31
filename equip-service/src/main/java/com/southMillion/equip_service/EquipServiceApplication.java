package com.SouthMillion.equip_service;

import com.SouthMillion.equip_service.config.EquipmentConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class EquipServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipServiceApplication.class, args);
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public static class CacheInitializer {
        private final EquipmentConfigCache equipmentConfigCache;

        @EventListener(ApplicationReadyEvent.class)
        public void initializeCache() {
            try {
                log.info("Initializing EquipmentConfigCache on startup...");
                equipmentConfigCache.ensureLoaded();
                log.info("EquipmentConfigCache initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize EquipmentConfigCache: {}", e.getMessage());
            }
        }
    }
}
