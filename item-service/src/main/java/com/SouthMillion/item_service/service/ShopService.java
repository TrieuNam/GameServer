package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.ShopEntity;
import com.SouthMillion.item_service.repository.ShopRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.ShopDTO;
import org.SouthMillion.dto.item.ShopEvent;
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
public class ShopService {
    @Autowired
    private ShopRepository shopRepo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private KafkaTemplate<String, ShopEvent> kafka;

    private static final String SHOP_LIMIT_KEY = "shop:limit:%d";

    public List<ShopDTO> getLimit(Long userId) {
        String key = String.format(SHOP_LIMIT_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseJsonList(cache);

        List<ShopEntity> list = shopRepo.findByUserId(userId);
        List<ShopDTO> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        redis.opsForValue().set(key, toJson(dtos), Duration.ofMinutes(5));
        return dtos;
    }

    public void buy(Long userId, Integer index, Integer num) {
        int maxBuy = 5;
        Optional<ShopEntity> optional = shopRepo.findByUserIdAndItemIndex(userId, index);
        ShopEntity entity = optional.orElse(null);

        if (entity == null) {
            if (num > maxBuy)
                throw new RuntimeException("Vượt quá giới hạn mua");
            entity = new ShopEntity();
            entity.setUserId(userId);
            entity.setItemIndex(index);
            entity.setBuyNum(num);
        } else {
            if (entity.getBuyNum() + num > maxBuy)
                throw new RuntimeException("Vượt quá giới hạn mua");
            entity.setBuyNum(entity.getBuyNum() + num);
        }
        shopRepo.save(entity);

        List<ShopEntity> all = shopRepo.findByUserId(userId);
        List<ShopDTO> dtos = all.stream().map(this::toDto).collect(Collectors.toList());
        redis.opsForValue().set(String.format(SHOP_LIMIT_KEY, userId), toJson(dtos), Duration.ofMinutes(5));

        kafka.send("shop-event", new ShopEvent(userId, index, num, "BUY"));
    }

    private ShopDTO toDto(ShopEntity entity) {
        ShopDTO dto = new ShopDTO();
        dto.setItemIndex(entity.getItemIndex());
        dto.setBuyNum(entity.getBuyNum());
        return dto;
    }
    private List<ShopDTO> parseJsonList(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(json, ShopDTO[].class));
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
