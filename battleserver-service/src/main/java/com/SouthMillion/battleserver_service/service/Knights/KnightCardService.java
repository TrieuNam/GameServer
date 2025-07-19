package com.SouthMillion.battleserver_service.service.Knights;

import com.SouthMillion.battleserver_service.dto.knights.knight_card.KnightCardDto;
import com.SouthMillion.battleserver_service.dto.knights.knight_card.KnightZhengDto;
import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KnightCardService {

    @Autowired
    private ConfigServiceClient configServiceClient;
    @Autowired
    private ObjectMapper objectMapper;

    public List<KnightCardDto> getAllKnightCards() {
        JsonNode json = configServiceClient.getKnightCardConfigFile();
        JsonNode arr = json.get("knight_card");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<KnightCardDto> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, KnightCardDto.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public List<KnightZhengDto> getAllKnightZhengs() {
        JsonNode json = configServiceClient.getKnightCardConfigFile();
        JsonNode arr = json.get("knight_zheng");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<KnightZhengDto> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, KnightZhengDto.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public KnightZhengDto getKnightZhengBySeq(String seq) {
        JsonNode json = configServiceClient.getKnightCardConfigFile();
        JsonNode arr = json.get("knight_zheng");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                try {
                    return objectMapper.treeToValue(node, KnightZhengDto.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}