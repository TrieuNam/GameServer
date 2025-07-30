package com.SouthMillion.item_service.service.config;

import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.Box.BXJJConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoteBXJJConfigService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    public BXJJConfigDTO getConfig() {
        JsonNode json = configFeignClient.getConfigFile("baoxiangjijin");
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.treeToValue(json, BXJJConfigDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot map unpack.json", e);
        }
    }
}
