package com.southMillion.equip_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.southMillion.equip_service.entity.GemEntity;
import com.southMillion.equip_service.repository.GemRepository;
import org.SouthMillion.dto.item.GemDTO;
import org.SouthMillion.dto.item.GemInlayDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GemService {
    @Autowired
    private GemRepository repo;
    @Autowired private StringRedisTemplate redis;
    private static final String GEM_KEY = "gem:%d";

    public GemDTO getInfo(Long userId) {
        String key = String.format(GEM_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        GemEntity e = repo.findById(userId).orElseGet(() -> {
            GemEntity entity = new GemEntity();
            entity.setUserId(userId);
            entity.setDrawingId(-1);
            entity.setGemListJson("[]");
            repo.save(entity);
            return entity;
        });
        GemDTO dto = toDto(e);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    // Gắn, tháo, nâng bảo thạch...
    public void operate(Long userId, Integer opType, Integer param1, Integer param2) {
        GemEntity entity = repo.findById(userId).orElseThrow();
        List<GemInlayDTO> gems = parseList(entity.getGemListJson());

        if (opType == 1) { // Gắn mới
            gems.add(new GemInlayDTO(param1, param2)); // param1: itemId, param2: pos
        } else if (opType == 2) { // Tháo
            gems.removeIf(g -> g.getPos().equals(param2)); // param2: pos
        }
        entity.setGemListJson(toJson(gems));
        repo.save(entity);
        redis.delete(String.format(GEM_KEY, userId));
    }

    private GemDTO toDto(GemEntity e) {
        GemDTO dto = new GemDTO();
        dto.setDrawingId(e.getDrawingId());
        dto.setGemList(parseList(e.getGemListJson()));
        return dto;
    }
    private List<GemInlayDTO> parseList(String json) {
        try { return Arrays.asList(new ObjectMapper().readValue(json, GemInlayDTO[].class)); }
        catch (Exception ex) { return new ArrayList<>(); }
    }
    private String toJson(Object o) {
        try { return new ObjectMapper().writeValueAsString(o); }
        catch (Exception ex) { return "[]"; }
    }
    private GemDTO parse(String json) {
        try { return new ObjectMapper().readValue(json, GemDTO.class); }
        catch (Exception ex) { return new GemDTO(); }
    }
}
