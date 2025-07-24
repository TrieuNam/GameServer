package com.SouthMillion.battleserver_service.service.monsters;

import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.NotFoundException;
import org.SouthMillion.dto.battle.MonsterGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MonsterGroupService {
    @Autowired
    private ConfigServiceClient configServiceClient;

    public List<MonsterGroupDTO> getAllMonsterGroups() {
        JsonNode root = configServiceClient.getConfigFile();
        JsonNode arr = root.get("monster_group");
        // Chuyển JsonNode sang List<MonsterGroupDTO>
        List<MonsterGroupDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                MonsterGroupDTO dto = new MonsterGroupDTO();
                dto.setMonsterGroupId(node.get("monster_group_id").asText());
                dto.setName(node.get("name").asText());
                dto.setMonsterId0(node.get("monster_id_0").asText());
                dto.setMonsterId1(node.get("monster_id_1").asText());
                dto.setMonsterId2(node.get("monster_id_2").asText());
                result.add(dto);
            }
        }
        return result;
    }


    public MonsterGroupDTO getMonsterGroupById(String monsterGroupId) {
        JsonNode json = configServiceClient.getConfigFile(); // ObjectNode

        // Lấy ra mảng group
        JsonNode arr = json.get("monster_group");
        if (arr == null || !arr.isArray()) {
            throw new NotFoundException("Field 'monster_group' không tồn tại hoặc không phải mảng!");
        }

        // Duyệt từng phần tử mảng để tìm group cần
        for (JsonNode group : arr) {
            JsonNode idNode = group.get("monster_group_id");
            if (idNode != null && idNode.asText().equals(monsterGroupId)) {
                return MonsterGroupDTO.builder()
                        .monsterGroupId(idNode.asText())
                        .name(safeGetText(group, "name"))
                        .monsterId0(safeGetText(group, "monster_id_0"))
                        .monsterId1(safeGetText(group, "monster_id_1"))
                        .monsterId2(safeGetText(group, "monster_id_2"))
                        .build();
            }
        }
        throw new NotFoundException("Monster group not found: " + monsterGroupId);
    }

    // Helper an toàn
    private String safeGetText(JsonNode node, String field) {
        JsonNode subNode = node.get(field);
        return (subNode != null && !subNode.isNull()) ? subNode.asText() : null;
    }

}