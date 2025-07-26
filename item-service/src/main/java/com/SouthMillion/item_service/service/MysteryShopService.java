package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.MysteryShopEntity;
import com.SouthMillion.item_service.repository.MysteryShopRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private KafkaTemplate<String, MysteryShopEvent> kafka;

    private static final String MYSTERY_SHOP_KEY = "mysteryshop:info:%d";
    private static final ObjectMapper mapper = new ObjectMapper();

    // Lấy info mystery shop cho user (có cache)
    public MysteryShopDTO getShopInfo(Long userId) {
        int uid = userId.intValue();
        String key = String.format(MYSTERY_SHOP_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseJson(cache);

        MysteryShopEntity entity = repo.findById(uid).orElseGet(() -> {
            MysteryShopEntity e = new MysteryShopEntity();
            e.setUserId(uid);
            e.setBuyFlag(0);
            e.setIndexListJson("[]"); // chưa có shop nào
            repo.save(e);
            return e;
        });
        MysteryShopDTO dto = MysteryShopEntity.fromEntity(entity);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(5));
        return dto;
    }

    // Mua item (index trong indexList)
    public void buy(Long userId, Integer index) {
        int uid = userId.intValue();
        MysteryShopEntity entity = repo.findById(uid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop info user"));
        List<Integer> indexList = parseIndexes(entity.getIndexListJson());

        // Kiểm tra index hợp lệ
        if (!indexList.contains(index))
            throw new RuntimeException("Hàng không tồn tại trong shop user");

        // Kiểm tra đã mua chưa bằng bitmask buyFlag
        int bit = 1 << index;
        if ((entity.getBuyFlag() & bit) != 0)
            throw new RuntimeException("Đã mua item này");

        entity.setBuyFlag(entity.getBuyFlag() | bit);
        repo.save(entity);

        redis.delete(String.format(MYSTERY_SHOP_KEY, userId));
        kafka.send("mysteryshop-event", new MysteryShopEvent(userId, index, 1, "BUY"));
    }

    // Đổi shop mới (random indexList, reset buyFlag)
    public void refreshShop(Long userId, List<Integer> newIndexes) {
        int uid = userId.intValue();
        MysteryShopEntity entity = repo.findById(uid).orElseThrow();
        entity.setBuyFlag(0);
        entity.setIndexListJson(toJson(newIndexes));
        repo.save(entity);
        redis.delete(String.format(MYSTERY_SHOP_KEY, uid));
    }

    // Helper parse
    public static List<Integer> parseIndexes(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<Integer>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private MysteryShopDTO parseJson(String json) {
        try {
            return mapper.readValue(json, MysteryShopDTO.class);
        } catch (Exception e) {
            return new MysteryShopDTO();
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}