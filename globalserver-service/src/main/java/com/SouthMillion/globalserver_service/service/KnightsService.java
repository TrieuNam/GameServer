package com.SouthMillion.globalserver_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.globalserver.KnightsDTO;
import org.SouthMillion.dto.globalserver.KnightsEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class KnightsService {
    @Autowired
    private StringRedisTemplate redis;
    @Autowired private KafkaTemplate<String, KnightsEvent> kafka;

    private static final String KNIGHTS_KEY = "knights:info:%d";

    public KnightsDTO getKnightsInfo(Long userId) {
        String key = String.format(KNIGHTS_KEY, userId);
        String json = redis.opsForValue().get(key);
        if (json != null) return parse(json);

        KnightsDTO dto = new KnightsDTO();
        dto.setLevel(1);
        dto.setFlag(0);
        dto.setLevelFlag(0);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    public void operate(Long userId, Integer opType, Integer param1) {
        KnightsDTO dto = getKnightsInfo(userId);

        if (opType == 1) { // Up level
            dto.setLevel(dto.getLevel() + 1);
        } else if (opType == 2) { // Set flag
            dto.setFlag(param1);
        }
        redis.opsForValue().set(String.format(KNIGHTS_KEY, userId), toJson(dto), Duration.ofMinutes(10));
        kafka.send("knights-event", new KnightsEvent(userId, opType, param1));
    }

    private KnightsDTO parse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, KnightsDTO.class);
        } catch (Exception e) {
            return new KnightsDTO();
        }
    }
    private String toJson(Object o) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}