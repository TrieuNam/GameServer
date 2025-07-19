package com.example.ArenaService.service.arena;

import com.example.ArenaService.dto.arena.ArenaCfgDTO;
import com.example.ArenaService.dto.arena.AwardDTO;
import com.example.ArenaService.dto.arena.MonsterDTO;
import com.example.ArenaService.dto.arena.WeeklyJoinAwardDTO;
import com.example.ArenaService.service.client.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArenaService {
    @Autowired
    private ConfigServiceClient configServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    public JsonNode getJson() {
        return configServiceClient.getArenaConfigJson();
    }

    // arena_cfg
    public List<ArenaCfgDTO> getArenaCfgList() throws Exception {
        JsonNode arr = getJson().get("arena_cfg");
        List<ArenaCfgDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, ArenaCfgDTO.class));
            }
        }
        return result;
    }

    public ArenaCfgDTO getArenaCfgByInitialNum(String initialNum) throws Exception {
        JsonNode arr = getJson().get("arena_cfg");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (initialNum.equals(node.get("initial_num").asText())) {
                return objectMapper.treeToValue(node, ArenaCfgDTO.class);
            }
        }
        return null;
    }

    // everyday_award
    public List<AwardDTO> getEverydayAwardList() throws Exception {
        JsonNode arr = getJson().get("everyday_award");
        List<AwardDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, AwardDTO.class));
            }
        }
        return result;
    }
    public AwardDTO getEverydayAwardBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("everyday_award");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, AwardDTO.class);
            }
        }
        return null;
    }

    // weekly_award
    public List<AwardDTO> getWeeklyAwardList() throws Exception {
        JsonNode arr = getJson().get("weekly_award");
        List<AwardDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, AwardDTO.class));
            }
        }
        return result;
    }
    public AwardDTO getWeeklyAwardBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("weekly_award");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, AwardDTO.class);
            }
        }
        return null;
    }

    // weekly_join_award
    public List<WeeklyJoinAwardDTO> getWeeklyJoinAwardList() throws Exception {
        JsonNode arr = getJson().get("weekly_join_award");
        List<WeeklyJoinAwardDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, WeeklyJoinAwardDTO.class));
            }
        }
        return result;
    }
    public WeeklyJoinAwardDTO getWeeklyJoinAwardBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("weekly_join_award");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, WeeklyJoinAwardDTO.class);
            }
        }
        return null;
    }

    // arena_monster
    public List<MonsterDTO> getArenaMonsterList() throws Exception {
        JsonNode arr = getJson().get("arena_monster");
        List<MonsterDTO> result = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                result.add(objectMapper.treeToValue(node, MonsterDTO.class));
            }
        }
        return result;
    }
    public MonsterDTO getArenaMonsterBySeq(String seq) throws Exception {
        JsonNode arr = getJson().get("arena_monster");
        if (arr == null || !arr.isArray()) return null;
        for (JsonNode node : arr) {
            if (seq.equals(node.get("seq").asText())) {
                return objectMapper.treeToValue(node, MonsterDTO.class);
            }
        }
        return null;
    }
}