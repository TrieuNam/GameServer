package com.SouthMillion.config_service;

import com.SouthMillion.config_service.config.ConfigProps;
import com.SouthMillion.config_service.config.CorsProps;
import com.SouthMillion.config_service.config.InternalProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


/**
 * WHAT: Ứng dụng Spring Boot của config-service.
 * WHY: Điểm khởi động service.
 * WHERE: entrypoint của service.
 * HOW: Bật @EnableConfigurationProperties để đọc cấu hình 'config.*' từ application.yml.
 */
@SpringBootApplication
@EnableConfigurationProperties({ConfigProps.class, CorsProps.class, InternalProps.class})
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}
