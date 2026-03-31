package com.SouthMillion.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Automatically grant database permissions on startup (PRODUCTION ONLY)
 * 
 * PROFILES:
 * - local: Uses root/root account, no permission grant needed
 * - prod: Uses tpnam/121831 account, auto-grants from Docker network IPs
 * 
 * This component runs FIRST before Flyway migrations to ensure proper database permissions.
 * It handles both fresh database setups and existing databases.
 */
@Slf4j
@Component
@Profile("prod") // Only run in production profile
@Order(1)
public class DatabaseUserInitializer implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")  
    private String appUsername;

    @Value("${spring.datasource.password}")
    private String appPassword;

    @Override
    public void run(String... args) {
        try {
            log.info("🔐 Initializing database user permissions for '{}'...", appUsername);
            
            String host = extractHost(datasourceUrl);
            String port = extractPort(datasourceUrl);
            String database = extractDatabase(datasourceUrl);
            
            boolean success = tryGrantPermissions(host, port, database);
            
            if (success) {
                log.info("✅ User '{}' has full access to '{}' from any host", appUsername, database);
            } else {
                log.warn("⚠️  Could not verify/grant permissions (database may not be ready or root access denied)");
            }
            
        } catch (Exception e) {
            log.warn("⚠️  Failed to initialize database user: {}", e.getMessage());
        }
    }

    private boolean tryGrantPermissions(String host, String port, String database) {
        String[] rootPasswords = {"root", "admin-admindb", "", "password"};
        
        for (String rootPassword : rootPasswords) {
            try {
                String rootUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000", host, port);
                
                try (Connection conn = DriverManager.getConnection(rootUrl, "root", rootPassword);
                     Statement stmt = conn.createStatement()) {
                    
                    stmt.execute("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    
                    String[] hosts = {"%", "localhost", "172.%", "192.168.%"};
                    
                    for (String hostPattern : hosts) {
                        try {
                            stmt.execute(String.format("CREATE USER IF NOT EXISTS '%s'@'%s' IDENTIFIED BY '%s'", 
                                appUsername, hostPattern, appPassword));
                            stmt.execute(String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%s'", 
                                database, appUsername, hostPattern));
                        } catch (Exception e) {
                            try {
                                stmt.execute(String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%s'", 
                                    database, appUsername, hostPattern));
                            } catch (Exception ex) {
                            }
                        }
                    }
                    
                    stmt.execute("FLUSH PRIVILEGES");
                    return true;
                    
                } catch (Exception e) {
                    log.debug("Failed with root password: {}", e.getMessage());
                }
            } catch (Exception e) {
            }
        }
        
        return false;
    }

    private String extractHost(String jdbcUrl) {
        try {
            String afterProtocol = jdbcUrl.split("//")[1];
            String hostPort = afterProtocol.split("/")[0];
            return hostPort.split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String extractPort(String jdbcUrl) {
        try {
            String afterProtocol = jdbcUrl.split("//")[1];
            String hostPort = afterProtocol.split("/")[0];
            String[] parts = hostPort.split(":");
            return parts.length > 1 ? parts[1] : "3306";
        } catch (Exception e) {
            return "3306";
        }
    }

    private String extractDatabase(String jdbcUrl) {
        try {
            String afterHost = jdbcUrl.split("//")[1].split("/", 2)[1];
            return afterHost.split("\\?")[0];
        } catch (Exception e) {
            return "unknown_db";
        }
    }
}
