package com.example.ArenaService.service.arena;

import com.example.ArenaService.dto.arena.AwardDTO;
import com.example.ArenaService.dto.arena.DfArenaCfgDTO;
import com.example.ArenaService.service.client.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DfArenaService {
    @Autowired
    private ConfigServiceClient configServiceClient;
    @Autowired
    private ObjectMapper objectMapper;

    public JsonNode getJson() {
        return configServiceClient.getDfArenaConfigJson();
    }

    // df_arena_cfg
    public List<DfArenaCfgDTO> getDfArenaCfgList() throws Exception {
        JsonNode arr = getJson().get("df_arena_cfg");
        List<DfArenaCfgDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, DfArenaCfgDTO.class));
            }
        }
        return result;
    }
    public DfArenaCfgDTO getDfArenaCfgByInitialNum(String initialNum) throws Exception {
        JsonNode arr = getJson().get("df_arena_cfg");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (initialNum.equals(node.get("initial_num").asText())) {
                return objectMapper.treeToValue(node, DfArenaCfgDTO.class);
            }
        }
        return null;
    }

    // df_everyday_award
    public List<AwardDTO> getDfEverydayAwardList() throws Exception {
        JsonNode arr = getJson().get("df_everyday_award");
        List<AwardDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, AwardDTO.class));
            }
        }
        return result;
    }
    public AwardDTO getDfEverydayAwardBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("df_everyday_award");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, AwardDTO.class);
            }
        }
        return null;
    }

    // df_weekly_award
    public List<AwardDTO> getDfWeeklyAwardList() throws Exception {
        JsonNode arr = getJson().get("df_weekly_award");
        List<AwardDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, AwardDTO.class));
            }
        }
        return result;
    }
    public AwardDTO getDfWeeklyAwardBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("df_weekly_award");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, AwardDTO.class);
            }
        }
        return null;
    }
}