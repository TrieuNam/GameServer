package com.SouthMillion.item_service.service.config;

import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.knights.HarnessItemConfigDTO;
import org.SouthMillion.dto.item.knights.KnightCardConfigDTO;
import org.SouthMillion.dto.item.knights.KnightsConfigDTO;
import org.SouthMillion.dto.item.knights.MountConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonKnightConfigService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cách 1: Hàm generic, dùng cho mọi DTO
    public <T> T getConfig(String fileName, Class<T> clazz) {
        JsonNode jsonNode = configFeignClient.getConfigFile(fileName);
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi parse config: " + fileName, e);
        }
    }

    // Cách 2: Viết hàm riêng cho từng DTO cho dễ đọc
    public KnightsConfigDTO getKnightsConfig() {
        return getConfig("knights", KnightsConfigDTO.class);
    }

    public KnightCardConfigDTO getKnightCardConfig() {
        return getConfig("knight_card", KnightCardConfigDTO.class);
    }

    public MountConfigDTO getMountConfig() {
        return getConfig("mount", MountConfigDTO.class);
    }

    public HarnessItemConfigDTO getHarnessItemConfig() {
        return getConfig("harness_item", HarnessItemConfigDTO.class);
    }
}