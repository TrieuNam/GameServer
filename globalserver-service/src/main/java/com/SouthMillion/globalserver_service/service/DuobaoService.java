package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.DuobaoEntity;
import com.SouthMillion.globalserver_service.repository.DuobaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.globalserver.DuobaoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DuobaoService {
    @Autowired
    private DuobaoRepository repo;
    @Autowired private StringRedisTemplate redis;
    private static final String DUOBAO_KEY = "duobao:info:%d";

    public DuobaoDTO getInfo(Long userId) {
        String key = String.format(DUOBAO_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        DuobaoEntity entity = repo.findById(userId).orElseGet(() -> {
            DuobaoEntity e = new DuobaoEntity();
            e.setUserId(userId);
            e.setIntegral(0); e.setFetchFlag(0); e.setFreeRefreshNum(1);
            e.setFreeRefreshTime(0L); repo.save(e);
            return e;
        });
        DuobaoDTO dto = toDto(entity);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    // Quay thưởng
    public void draw(Long userId, Integer opType, Integer param1, Integer param2) {
        DuobaoEntity entity = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("No duobao info"));
        // Logic: cộng integral, set cờ đã nhận thưởng, trừ lượt free nếu dùng refresh
        // Ví dụ: opType=0: draw, opType=1: refresh
        if (opType == 0) {
            // Quay, cộng điểm ngẫu nhiên 10-20
            int reward = 10 + (int)(Math.random()*11);
            entity.setIntegral(entity.getIntegral() + reward);
        } else if (opType == 1) {
            // refresh
            if (entity.getFreeRefreshNum() <= 0)
                throw new RuntimeException("No free refresh left");
            entity.setFreeRefreshNum(entity.getFreeRefreshNum() - 1);
            entity.setFreeRefreshTime(System.currentTimeMillis() + 10*60*1000L);
        }
        repo.save(entity);
        redis.delete(String.format(DUOBAO_KEY, userId));
    }

    private DuobaoDTO toDto(DuobaoEntity e) {
        DuobaoDTO dto = new DuobaoDTO();
        dto.setIntegral(e.getIntegral());
        dto.setFetchFlag(e.getFetchFlag());
        dto.setFreeRefreshNum(e.getFreeRefreshNum());
        dto.setFreeRefreshTime(e.getFreeRefreshTime());
        return dto;
    }
    private String toJson(Object o) {
        try { return new ObjectMapper().writeValueAsString(o); }
        catch (Exception ex) { return "{}"; }
    }
    private DuobaoDTO parse(String json) {
        try { return new ObjectMapper().readValue(json, DuobaoDTO.class); }
        catch (Exception ex) { return new DuobaoDTO(); }
    }
}