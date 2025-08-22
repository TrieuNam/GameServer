package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AngelConfigService {

    @Autowired
    private  ConfigFeignClient configFeignClient;

    private  ObjectMapper objectMapper;

    // Cache config trong RAM (chỉ load 1 lần/lazy load)
    private AngelConfigDTO cachedConfig;

    /**
     * Tải và parse angel.json từ config-service
     */
    public AngelConfigDTO getAngelConfig() {
        if (cachedConfig == null) {
            JsonNode jsonNode = configFeignClient.getConfigFile("angel.json");
            try {
                cachedConfig = objectMapper.treeToValue(jsonNode, AngelConfigDTO.class);
            } catch (Exception e) {
                throw new RuntimeException("Parse angel.json thất bại!", e);
            }
        }
        return cachedConfig;
    }

    /**
     * Reload lại angel.json (nếu hot update)
     */
    public AngelConfigDTO reloadAngelConfig() {
        JsonNode jsonNode = configFeignClient.getConfigFile("angel.json");
        try {
            cachedConfig = objectMapper.treeToValue(jsonNode, AngelConfigDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Parse angel.json thất bại!", e);
        }
        return cachedConfig;
    }

    // --- Ví dụ: Truy vấn các thông tin theo nhu cầu ---
    public Optional<AngelConfigDTO.AngelLevelCfg> getAngelLevelCfg(int level) {
        return getAngelConfig().getAngelCfg().stream()
                .filter(cfg -> cfg.getLevel() == level)
                .findFirst();
    }

    public Optional<AngelConfigDTO.AngelUpCfg> getAngelStageCfg(int stage) {
        return getAngelConfig().getAngelUp().stream()
                .filter(cfg -> cfg.getAngleStage() == stage)
                .findFirst();
    }

    // ... các hàm get theo id/seq khác tuỳ nghiệp vụ ...
}