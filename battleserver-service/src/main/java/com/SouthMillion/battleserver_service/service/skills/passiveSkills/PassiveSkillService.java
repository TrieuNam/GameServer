package com.SouthMillion.battleserver_service.service.skills.passiveSkills;

import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.battle.PassiveSkillDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PassiveSkillService {

    @Autowired
    private ConfigServiceClient configServiceClient; // Feign lấy passive_skill.json từ config-service
    @Autowired
    private ObjectMapper objectMapper;

    public List<PassiveSkillDTO> getAllPassiveCfg() {
        JsonNode json = configServiceClient.getPassiveSkillConfigFile();
        JsonNode arr = json.get("passive_cfg");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<PassiveSkillDTO> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, PassiveSkillDTO.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public PassiveSkillDTO getPassiveCfgBySkillId(String skillId) {
        JsonNode json = configServiceClient.getPassiveSkillConfigFile();
        JsonNode arr = json.get("passive_cfg");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (skillId.equals(node.get("skill_id").asText())) {
                try {
                    return objectMapper.treeToValue(node, PassiveSkillDTO.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}