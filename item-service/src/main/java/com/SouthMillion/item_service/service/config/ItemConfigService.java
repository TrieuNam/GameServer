package com.SouthMillion.item_service.service.config;

import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.SouthMillion.dto.item.Knapsack.*;
import org.SouthMillion.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemConfigService {
    @Autowired private StringRedisTemplate redis;
    @Autowired private ConfigFeignClient configFeign;
    private static final String REDIS_KEY = "config:item:all";

    private static final Map<String, Class<?>> FILE_DTO_MAP = Map.of(
            "block_item", BlockItemConfigDTO.class,
            "debris", DebrisConfigDTO.class,
            "harness_item", HarnessItemConfigDTO.class,
            "inscription_item", InscriptionItemConfigDTO.class,
            "model_item", ModelItemConfigDTO.class,
            "pet_item", PetItemConfigDTO.class,
            "pet_weapon_item", PetWeaponItemConfigDTO.class,
            "scroll_item", ScrollItemConfigDTO.class,
            "title_item", TitleItemConfigDTO.class
            // "item_retrieve.json" nếu cần riêng, cấu trúc đặc biệt
    );

    // Map ALL itemId -> configDTO (bạn có thể lưu thành Map nếu muốn tối ưu lookup)
    public Map<Integer, Object> getAllAsMap() {
        String json = redis.opsForValue().get(REDIS_KEY);
        if (json != null) {
            // deserialize về Map itemId -> Object (bạn cần ghi rõ kiểu khi deserialize)
            return JsonUtil.fromJsonToMap(json, Integer.class, Object.class); // giả sử có util này
        }

        Map<Integer, Object> all = new HashMap<>();
        for (var entry : FILE_DTO_MAP.entrySet()) {
            String file = entry.getKey();
            Class<?> dtoClass = entry.getValue();
            try {
                JsonNode node = configFeign.getConfigFile("item/" + file);
                String arrKey = file.replace(".json", ""); // lấy key root của json (vd: block, debris,...)
                JsonNode arr = node.get(arrKey);
                if (arr != null && arr.isArray()) {
                    for (JsonNode itemNode : arr) {
                        Object dto = JsonUtil.fromJson(itemNode.toString(), dtoClass);
                        Integer id = Integer.valueOf(itemNode.get("id").asText());
                        all.put(id, dto);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading config " + file + ": " + e.getMessage());
            }
        }
        // item_retrieve.json có thể load và lưu riêng nếu bạn muốn

        redis.opsForValue().set(REDIS_KEY, JsonUtil.toJson(all), Duration.ofHours(2));
        return all;
    }

    public Object getConfigById(int itemId) {
        return getAllAsMap().get(itemId);
    }

    public ItemRetrieveConfigDTO getItemRetrieveConfig() {
        String key = "config:item:retrieve";
        String json = redis.opsForValue().get(key);
        if (json != null) {
            return JsonUtil.fromJson(json, ItemRetrieveConfigDTO.class);
        }
        try {
            JsonNode node = configFeign.getConfigFile("item_retrieve");
            ItemRetrieveConfigDTO dto = JsonUtil.fromJson(node.toString(), ItemRetrieveConfigDTO.class);
            redis.opsForValue().set(key, JsonUtil.toJson(dto), Duration.ofHours(2));
            return dto;
        } catch (Exception e) {
            System.err.println("Error loading item_retrieve.json: " + e.getMessage());
            return null;
        }
    }
}