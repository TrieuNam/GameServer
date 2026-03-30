package com.SouthMillion.config_service;

import com.SouthMillion.config_service.config.ConfigProps;
import com.SouthMillion.config_service.config.CorsProps;
import com.SouthMillion.config_service.config.InternalProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


/**
 * WHAT: á»¨ng dá»¥ng Spring Boot cá»§a config-service.
 * WHY: Äiá»ƒm khá»Ÿi Ä‘á»™ng service.
 * WHERE: entrypoint cá»§a service.
 * HOW: Báº­t @EnableConfigurationProperties Ä‘á»ƒ Ä‘á»c cáº¥u hÃ¬nh 'config.*' tá»« application.yml.
 */
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableConfigurationProperties({ConfigProps.class, CorsProps.class, InternalProps.class})
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}
