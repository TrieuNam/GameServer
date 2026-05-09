package com.SouthMillion.activity_service.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
// other imports...

@Repository
public class ConfigFileRepository {

    // Externalize the hardcoded value to application.properties
    @Value("${config.file.path}")
    private String configFilePath;

    public File getConfigFile() {
        // Use configFilePath instead of hardcoded value
        return new File(configFilePath);
    }

    // rest of the class...
}