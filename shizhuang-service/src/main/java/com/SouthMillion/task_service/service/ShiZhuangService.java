package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.entity.ShiZhuangEntity;
import com.SouthMillion.task_service.entity.model_clothes.PlayerClothesEntity;
import com.SouthMillion.task_service.mapper.PlayerClothesMapper;
import com.SouthMillion.task_service.repository.PlayerClothesRepository;
import com.SouthMillion.task_service.repository.ShiZhuangRepository;
import com.SouthMillion.task_service.service.cache.ShiZhuangCacheService;
import com.SouthMillion.task_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.*;
import org.SouthMillion.dto.item.shop.ClothShopItemDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiZhuangService {
    private final ShiZhuangRepository repo;
    private final ShiZhuangCacheService cache;

    private final PlayerClothesRepository playerClothesRepository;
    private final ConfigFeignClient configFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Load config từ model_clothes.json
    public List<ClothesDTO> loadClothesConfig() {
        JsonNode jsonNode = configFeignClient.getConfigFile("model_clothes");
        if (jsonNode == null || !jsonNode.has("clothes")) return Collections.emptyList();
        try {
            return Arrays.asList(objectMapper.treeToValue(jsonNode.get("clothes"), ClothesDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse model_clothes.json fail", e);
        }
    }

    // Load config nâng cấp
    public List<ClothesUpDTO> loadClothesUpConfig() {
        JsonNode jsonNode = configFeignClient.getConfigFile("model_clothes");
        if (jsonNode == null || !jsonNode.has("clothes_up")) return Collections.emptyList();
        try {
            return Arrays.asList(objectMapper.treeToValue(jsonNode.get("clothes_up"), ClothesUpDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse clothes_up fail", e);
        }
    }

    // 1. Lấy config thời trang từ config-service
    private ClothesDTO getClothesConfig(Integer clothesId) {
        JsonNode jsonNode = configFeignClient.getConfigFile("config/gameworld/logicconfig/model_clothes.json");
        if (jsonNode == null || !jsonNode.has("clothes")) throw new RuntimeException("Config not found");
        try {
            List<ClothesDTO> list = Arrays.asList(objectMapper.treeToValue(jsonNode.get("clothes"), ClothesDTO[].class));
            return list.stream().filter(c -> c.getClothesId().equals(clothesId)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Clothes config not found"));
        } catch (Exception e) {
            throw new RuntimeException("Parse model_clothes.json fail", e);
        }
    }

    // Load shop từ cloth_shop.json
    public List<ClothShopItemDTO> loadClothShopConfig() {
        JsonNode jsonNode = configFeignClient.getConfigFile("cloth_shop");
        try {
            ClothShopConfigDTO config = objectMapper.treeToValue(jsonNode, ClothShopConfigDTO.class);
            return config.getShop();
        } catch (Exception e) {
            throw new RuntimeException("Parse cloth_shop.json fail", e);
        }
    }

    // Mua thời trang
    // 2. Xử lý logic mua thời trang
    @Transactional
    public void buyClothes(String playyerId, Integer clothesId, Integer num, Integer buyMoney, Integer addPayGold, Integer buyParam2) {
        if (num == null || num <= 0) throw new IllegalArgumentException("num phải > 0");
        ClothesDTO cfg = getClothesConfig(clothesId);

        // TODO: Validate buyMoney/addPayGold/buyParam2 theo logic nghiệp vụ

        // Update vào bảng sở hữu thời trang
        PlayerClothesEntity owned = playerClothesRepository.findByPlayerIdAndClothesId(playyerId, clothesId)
                .orElse(PlayerClothesEntity.builder()
                        .playerId(playyerId)
                        .clothesId(clothesId)
                        .level(1)
                        .build());
        // Nếu đã có, có thể tăng level hoặc ignore (tuỳ logic game)
        playerClothesRepository.save(owned);

        // TODO: Trừ tiền, log giao dịch, trả thông báo nếu cần
    }


    // Mặc thời trang (nếu chưa có thì tạo mới)
    @Transactional
    public void wearClothes(String playerId, Integer clothesId) {
        playerClothesRepository.findByPlayerIdAndClothesId(playerId, clothesId)
                .or(() -> Optional.of(playerClothesRepository.save(PlayerClothesEntity.builder()
                        .playerId(playerId).clothesId(clothesId).level(1).build())));
    }

    // Nâng cấp thời trang
    @Transactional
    public void levelUpClothes(String playyerId, Integer clothesId) {
        PlayerClothesEntity entity = playerClothesRepository.findByPlayerIdAndClothesId(playyerId, clothesId)
                .orElseThrow(() -> new RuntimeException("Chưa sở hữu thời trang này"));
        List<ClothesUpDTO> upList = loadClothesUpConfig();
        int nextLevel = entity.getLevel() + 1;
        ClothesUpDTO upCfg = upList.stream()
                .filter(c -> c.getClothesId().equals(clothesId) && c.getLevel() == nextLevel)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config nâng cấp cho level tiếp theo"));
        // TODO: kiểm tra nguyên liệu, trừ nguyên liệu
        entity.setLevel(nextLevel);
        playerClothesRepository.save(entity);
    }

    // Lấy danh sách thời trang sở hữu
    public List<PlayerClothesDTO> getClothes(String playyerId) {
        List<PlayerClothesEntity> entities = playerClothesRepository.findByPlayerId(playyerId);
        return entities.stream()
                .map(PlayerClothesMapper::toDTO)
                .collect(Collectors.toList());
    }


    public ShiZhuangDto get(String userId, int id) {
        ShiZhuangEntity cached = cache.getFromCache(userId, id);
        if (cached != null) return toDto(cached);

        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(userId, id);
        if (entity.isPresent()) {
            cache.putToCache(entity.get());
            return toDto(entity.get());
        }
        return null;
    }

    public List<ShiZhuangDto> getAll(String userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public ShiZhuangDto addOrUpdate(ShiZhuangDto dto) {
        ShiZhuangEntity entity = repo.findByUserIdAndId(dto.getUserId(), dto.getId() != 0 ? dto.getId() : -1)
                .orElse(ShiZhuangEntity.builder().userId(dto.getUserId()).level(dto.getLevel()).build());
        entity.setLevel(dto.getLevel());
        ShiZhuangEntity saved = repo.save(entity);
        cache.putToCache(saved);

        // Produce Kafka event khi thêm hoặc update

        return toDto(saved);
    }

    public boolean delete(String userId, int id) {
        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(userId, id);
        if (entity.isPresent()) {
            repo.delete(entity.get());
            cache.removeFromCache(userId, id);

            // Produce Kafka event khi xóa
            return true;
        }
        return false;
    }

    public ShiZhuangDto toDto(ShiZhuangEntity entity) {
        return ShiZhuangDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .level(entity.getLevel())
                .build();
    }
}
