package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.MysteryShopEntity;
import com.SouthMillion.item_service.repository.MysteryShopRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.MysteryShopDTO;
import org.SouthMillion.dto.item.MysteryShopEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MysteryShopService {
    @Autowired
    private MysteryShopRepository repo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private KafkaTemplate<String, MysteryShopEvent> kafka;

    private static final String MYSTERY_SHOP_KEY = "mysteryshop:limit:%d";

    public List<MysteryShopDTO> getLimit(Long userId) {
        String key = String.format(MYSTERY_SHOP_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseJsonList(cache);

        List<MysteryShopEntity> list = repo.findByUserId(userId);
        List<MysteryShopDTO> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        redis.opsForValue().set(key, toJson(dtos), Duration.ofMinutes(5));
        return dtos;
    }

    public void buy(Long userId, Integer index, Integer num) {
        int maxBuy = 3;
        Optional<MysteryShopEntity> optional = repo.findByUserIdAndItemIndex(userId, index);
        MysteryShopEntity entity = optional.orElse(null);

        if (entity == null) {
            if (num > maxBuy)
                throw new RuntimeException("Vượt quá giới hạn mua");
            entity = new MysteryShopEntity();
            entity.setUserId(userId);
            entity.setItemIndex(index);
            entity.setBuyNum(num);
        } else {
            if (entity.getBuyNum() + num > maxBuy)
                throw new RuntimeException("Vượt quá giới hạn mua");
            entity.setBuyNum(entity.getBuyNum() + num);
        }
        repo.save(entity);

        List<MysteryShopEntity> all = repo.findByUserId(userId);
        List<MysteryShopDTO> dtos = all.stream().map(this::toDto).collect(Collectors.toList());
        redis.opsForValue().set(String.format(MYSTERY_SHOP_KEY, userId), toJson(dtos), Duration.ofMinutes(5));

        kafka.send("mysteryshop-event", new MysteryShopEvent(userId, index, num, "BUY"));
    }

    private MysteryShopDTO toDto(MysteryShopEntity entity) {
        MysteryShopDTO dto = new MysteryShopDTO();
        dto.setItemIndex(entity.getItemIndex());
        dto.setBuyNum(entity.getBuyNum());
        return dto;
    }
    private List<MysteryShopDTO> parseJsonList(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(json, MysteryShopDTO[].class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    private String toJson(Object o) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}