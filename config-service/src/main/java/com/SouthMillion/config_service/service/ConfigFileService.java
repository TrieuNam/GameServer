package com.SouthMillion.config_service.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ConfigFileService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tìm và đọc file JSON bất kỳ trong /config hoặc các subfolder, chỉ cần biết tên file (không cần folder)
     *
     * @param fileName tên file (bỏ .json)
     */
    public JsonNode readConfig(String fileName) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            // Tìm mọi file .json trong /config và các subfolder
            Resource[] resources = resolver.getResources("classpath*:config/**/*.json");
            for (Resource resource : resources) {
                String name = resource.getFilename();
                if (name != null && name.equals(fileName + ".json")) {
                    try (InputStream in = resource.getInputStream()) {
                        return objectMapper.readTree(in);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Không tìm thấy file
        return null;
    }
}
