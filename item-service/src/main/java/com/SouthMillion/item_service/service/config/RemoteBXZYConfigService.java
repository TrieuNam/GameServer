package com.SouthMillion.item_service.service.config;

import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.Box.BXZYConfigDTO;
import org.SouthMillion.dto.item.Box.UnpackConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoteBXZYConfigService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    public BXZYConfigDTO getConfig() {
        JsonNode json = configFeignClient.getConfigFile("baoxiangzhuangyuan");
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.treeToValue(json, BXZYConfigDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot map unpack.json", e);
        }
    }
}
