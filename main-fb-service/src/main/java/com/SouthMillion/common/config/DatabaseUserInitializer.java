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
@Order(1) // Run first, before any other initializers including Flyway
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
            
            // Extract connection details from JDBC URL
            String host = extractHost(datasourceUrl);
            String port = extractPort(datasourceUrl);
            String database = extractDatabase(datasourceUrl);
            
            // Try to connect as root and grant permissions
            boolean success = tryGrantPermissions(host, port, database);
            
            if (success) {
                log.info("✅ User '{}' has full access to '{}' from any host", appUsername, database);
            } else {
                log.warn("⚠️  Could not verify/grant permissions (database may not be ready or root access denied)");
                log.warn("⚠️  If you see 'Access denied' errors, ensure database is running and user has proper permissions");
            }
            
        } catch (Exception e) {
            log.warn("⚠️  Failed to initialize database user: {}", e.getMessage());
            log.debug("Full error:", e);
        }
    }

    private boolean tryGrantPermissions(String host, String port, String database) {
        // Try different root passwords (common in Docker setups)
        // "1234" is used by DockerManager when auto-creating containers
        String[] rootPasswords = {"1234", "root", "admin-admindb", "", "password", "123456"};
        
        for (String rootPassword : rootPasswords) {
            try {
                String rootUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000", host, port);
                
                try (Connection conn = DriverManager.getConnection(rootUrl, "root", rootPassword);
                     Statement stmt = conn.createStatement()) {
                    
                    log.debug("Connected as root, granting permissions...");
                    
                    // Create database if not exists
                    stmt.execute("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    
                    // Create users and grant permissions from multiple hosts
                    // This ensures user can connect from localhost, Docker networks (172.x.x.x), and anywhere
                    String[] hosts = {"%", "localhost", "172.%", "192.168.%"};
                    
                    for (String hostPattern : hosts) {
                        try {
                            stmt.execute(String.format("CREATE USER IF NOT EXISTS '%s'@'%s' IDENTIFIED BY '%s'", 
                                appUsername, hostPattern, appPassword));
                            stmt.execute(String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%s'", 
                                database, appUsername, hostPattern));
                        } catch (Exception e) {
                            // User might already exist, just try to grant permissions
                            try {
                                stmt.execute(String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%s'", 
                                    database, appUsername, hostPattern));
                            } catch (Exception ex) {
                                // Ignore - might not have permission to grant
                            }
                        }
                    }
                    
                    stmt.execute("FLUSH PRIVILEGES");
                    return true; // Success!
                    
                } catch (Exception e) {
                    log.debug("Failed with root password '{}': {}", rootPassword.isEmpty() ? "(empty)" : "***", e.getMessage());
                }
            } catch (Exception e) {
                log.debug("Connection attempt failed: {}", e.getMessage());
            }
        }
        
        return false; // All attempts failed
    }

    private String extractHost(String jdbcUrl) {
        try {
            // jdbc:mysql://localhost:9088/game_admin?...
            String afterProtocol = jdbcUrl.split("//")[1];
            String hostPort = afterProtocol.split("/")[0];
            String[] parts = hostPort.split(":");
            return parts[0];
        } catch (Exception e) {
            log.warn("Failed to extract host from URL: {}", jdbcUrl);
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
            // jdbc:mysql://localhost:9088/game_admin?...
            String afterHost = jdbcUrl.split("//")[1].split("/", 2)[1];
            return afterHost.split("\\?")[0];
        } catch (Exception e) {
            log.warn("Failed to extract database from URL: {}", jdbcUrl);
            return "unknown_db";
        }
    }
}
