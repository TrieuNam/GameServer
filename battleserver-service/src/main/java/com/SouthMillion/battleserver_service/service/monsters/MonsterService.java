package com.SouthMillion.battleserver_service.service.monsters;

import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.MonsterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MonsterService {

    @Autowired
    private ConfigServiceClient configServiceClient;
    @Autowired
    private ObjectMapper objectMapper;

    public List<MonsterDTO> getAllMonsters() {
        JsonNode json = configServiceClient.getMonsterConfigFile();
        JsonNode arr = json.get("monster");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<MonsterDTO> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                MonsterDTO dto = objectMapper.treeToValue(node, MonsterDTO.class);
                list.add(dto);
            } catch (Exception e) {
                // Có thể log nếu cần
            }
        }
        return list;
    }

    public MonsterDTO getMonsterById(String monsterId) {
        JsonNode json = configServiceClient.getMonsterConfigFile();
        JsonNode arr = json.get("monster");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (monsterId.equals(node.get("monster_id").asText())) {
                try {
                    return objectMapper.treeToValue(node, MonsterDTO.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}
