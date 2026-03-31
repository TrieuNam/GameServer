package com.SouthMillion.mail_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mail Service Application
 * 
 * Features:
 * - System mail
 * - Player mail
 * - Mail with attachments (items, gold, gems)
 * - Auto-delete old mail
 * - Bulk mail sending
 * - Mail templates
 * 
 * @author LHP Game Server
 * @version 1.0.0
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableFeignClients
@EnableJpaAuditing
public class MailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Mail Service Started Successfully!");
        System.out.println("Port: 8470");
        System.out.println("Database: mail_db");
        System.out.println("========================================");
    }
}
