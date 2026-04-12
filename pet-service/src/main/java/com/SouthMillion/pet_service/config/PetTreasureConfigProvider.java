package com.SouthMillion.pet_service.config;

import com.SouthMillion.pet_service.client.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class PetTreasureConfigProvider {

    private final ConfigServiceClient configServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${pet.config.path:gameworld/logicconfig/pet.json}")
    private String configPath;

    private JsonNode cachedConfig;
    private String lastETag;

    public JsonNode getConfig() {
        if (cachedConfig != null) {
            return cachedConfig;
        }
        return reload();
    }

    public JsonNode reload() {
        try {
            ResponseEntity<byte[]> response = configServiceClient.getFile(configPath, lastETag);
            if (response.getStatusCode().value() == 304 && cachedConfig != null) {
                return cachedConfig;
            }
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedConfig = objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
                lastETag = response.getHeaders().getETag();
                return cachedConfig;
            }
        } catch (Exception e) {
            log.warn("Failed to load pet treasure config: {}", e.getMessage());
        }
        return cachedConfig;
    }
}