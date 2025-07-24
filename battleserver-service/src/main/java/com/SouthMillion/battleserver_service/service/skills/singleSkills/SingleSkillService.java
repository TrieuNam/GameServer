package com.SouthMillion.battleserver_service.service.skills.singleSkills;

import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.battle.SingleSkillDTO;
import org.SouthMillion.dto.battle.SingleSkillEffectDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SingleSkillService {

    @Autowired
    private ConfigServiceClient configServiceClient; // Feign lấy single_skill.json từ config-service
    @Autowired
    private ObjectMapper objectMapper;

    public List<SingleSkillEffectDTO> getAllSkillEff() {
        JsonNode json = configServiceClient.getSingleSkillConfigFile();
        JsonNode arr = json.get("skill_eff");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<SingleSkillEffectDTO> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, SingleSkillEffectDTO.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public SingleSkillEffectDTO getSkillEffBySeq(String seq) {
        JsonNode json = configServiceClient.getSingleSkillConfigFile();
        JsonNode arr = json.get("skill_eff");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                try {
                    return objectMapper.treeToValue(node, SingleSkillEffectDTO.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public List<SingleSkillDTO> getAllSkillCfg() {
        JsonNode json = configServiceClient.getSingleSkillConfigFile();
        JsonNode arr = json.get("skill_cfg");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<SingleSkillDTO> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, SingleSkillDTO.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public SingleSkillDTO getSkillCfgBySkillId(String skillId) {
        JsonNode json = configServiceClient.getSingleSkillConfigFile();
        JsonNode arr = json.get("skill_cfg");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (skillId.equals(node.get("skill_id").asText())) {
                try {
                    return objectMapper.treeToValue(node, SingleSkillDTO.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}