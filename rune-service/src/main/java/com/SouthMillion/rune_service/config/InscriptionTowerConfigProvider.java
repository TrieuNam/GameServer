package com.SouthMillion.rune_service.config;

import com.SouthMillion.rune_service.client.ConfigServiceClient;
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
public class InscriptionTowerConfigProvider {

    private final ConfigServiceClient configServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${rune.box.config.path:gameworld/logicconfig/inscription_tower.json}")
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
            log.warn("Failed to load inscription tower config: {}", e.getMessage());
        }
        return cachedConfig;
    }
}