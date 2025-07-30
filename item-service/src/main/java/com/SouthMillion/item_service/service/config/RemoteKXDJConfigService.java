package com.SouthMillion.item_service.service.config;

import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.Box.KXDJConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoteKXDJConfigService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    public KXDJConfigDTO getConfig() {
        JsonNode json = configFeignClient.getConfigFile("kaixiangdaji");
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.treeToValue(json, KXDJConfigDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot map unpack.json", e);
        }
    }
}
