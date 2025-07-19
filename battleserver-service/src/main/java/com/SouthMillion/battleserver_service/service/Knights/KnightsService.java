package com.SouthMillion.battleserver_service.service.Knights;


import com.SouthMillion.battleserver_service.dto.knights.knight.KnightsBookDto;
import com.SouthMillion.battleserver_service.dto.knights.knight.KnightsRewardDto;
import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KnightsService {

    @Autowired
    private ConfigServiceClient configServiceClient;
    @Autowired
    private ObjectMapper objectMapper;

    public List<KnightsBookDto> getAllKnightsBook() {
        JsonNode json = configServiceClient.getKnightsConfigFile();
        JsonNode arr = json.get("knights_book");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<KnightsBookDto> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                KnightsBookDto dto = objectMapper.treeToValue(node, KnightsBookDto.class);
                list.add(dto);
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public List<KnightsRewardDto> getAllKnightsReward() {
        JsonNode json = configServiceClient.getKnightsConfigFile();
        JsonNode arr = json.get("knights_reward");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<KnightsRewardDto> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                KnightsRewardDto dto = objectMapper.treeToValue(node, KnightsRewardDto.class);
                list.add(dto);
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public KnightsBookDto getKnightsBookByLevelAndSeq(String level, String seq) {
        JsonNode json = configServiceClient.getKnightsConfigFile();
        JsonNode arr = json.get("knights_book");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (level.equals(node.get("level").asText()) && seq.equals(node.get("seq").asText())) {
                try {
                    return objectMapper.treeToValue(node, KnightsBookDto.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public KnightsRewardDto getKnightsRewardByLevel(String level) {
        JsonNode json = configServiceClient.getKnightsConfigFile();
        JsonNode arr = json.get("knights_reward");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (level.equals(node.get("level").asText())) {
                try {
                    return objectMapper.treeToValue(node, KnightsRewardDto.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}