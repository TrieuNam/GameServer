package com.SouthMillion.arenaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.SouthMillion.arenaservice", "com.SouthMillion.common"})
@EnableFeignClients(basePackages = "com.SouthMillion.arenaservice")
@EnableJpaRepositories
@EnableCaching
@EnableAsync
@EnableScheduling
public class ArenaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArenaServiceApplication.class, args);
    }
}
