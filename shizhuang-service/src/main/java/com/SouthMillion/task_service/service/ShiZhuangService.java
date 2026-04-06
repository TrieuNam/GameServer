package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.entity.ShiZhuangEntity;
import com.SouthMillion.task_service.entity.model_clothes.PlayerClothesEntity;
import com.SouthMillion.task_service.mapper.PlayerClothesMapper;
import com.SouthMillion.task_service.repository.PlayerClothesRepository;
import com.SouthMillion.task_service.repository.ShiZhuangRepository;
import com.SouthMillion.task_service.service.cache.ShiZhuangCacheService;
import com.SouthMillion.task_service.service.client.ConfigFeignClient;
import com.SouthMillion.shizhuang_service.client.WalletFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.ShiZhuang.*;
import org.SouthMillion.dto.item.shop.ClothShopItemDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiZhuangService {
    private static final String MODEL_CLOTHES_PATH = "gameworld/logicconfig/model_clothes.json";
    private static final String CLOTH_SHOP_PATH = "gameworld/logicconfig/cloth_shop.json";

    private final ShiZhuangRepository repo;
    private final ShiZhuangCacheService cache;

    private final PlayerClothesRepository playerClothesRepository;
    private final ConfigFeignClient configFeignClient;
    private final WalletFeignClient walletFeignClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${shizhuang.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${shizhuang.config.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${shizhuang.config.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    private final Map<String, JsonNode> localConfigCache = new ConcurrentHashMap<>();

    // Virtual Thread executor for parallel operations
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Load config từ model_clothes.json
    public List<ClothesDTO> loadClothesConfig() {
        JsonNode jsonNode = loadConfigNode(MODEL_CLOTHES_PATH);
        if (jsonNode == null || !jsonNode.has("clothes")) return Collections.emptyList();
        try {
            return Arrays.asList(objectMapper.treeToValue(jsonNode.get("clothes"), ClothesDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse model_clothes.json fail", e);
        }
    }

    // Load config nâng cấp
    public List<ClothesUpDTO> loadClothesUpConfig() {
        JsonNode jsonNode = loadConfigNode(MODEL_CLOTHES_PATH);
        if (jsonNode == null || !jsonNode.has("clothes_up")) return Collections.emptyList();
        try {
            return Arrays.asList(objectMapper.treeToValue(jsonNode.get("clothes_up"), ClothesUpDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse clothes_up fail", e);
        }
    }

    // 1. Lấy config thời trang từ Redis-first cache
    private ClothesDTO getClothesConfig(Integer clothesId) {
        JsonNode jsonNode = loadConfigNode(MODEL_CLOTHES_PATH);
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
        JsonNode jsonNode = loadConfigNode(CLOTH_SHOP_PATH);
        if (jsonNode == null || jsonNode.isNull()) {
            return Collections.emptyList();
        }
        try {
            ClothShopConfigDTO config = objectMapper.treeToValue(jsonNode, ClothShopConfigDTO.class);
            return config != null && config.getShop() != null ? config.getShop() : Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Parse cloth_shop.json fail", e);
        }
    }

    private JsonNode loadConfigNode(String path) {
        JsonNode localCached = localConfigCache.get(path);
        if (localCached != null && !localCached.isNull()) {
            return localCached;
        }

        String redisKey = toRedisKey(path);
        if (redisEnabled) {
            try {
                String cached = redis.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    JsonNode node = objectMapper.readTree(cached);
                    localConfigCache.put(path, node);
                    touchRedisKey(redisKey);
                    log.debug("[ShiZhuangService] Redis HIT path={}", path);
                    return node;
                }
                log.debug("[ShiZhuangService] Redis MISS path={}", path);
            } catch (Exception e) {
                log.warn("[ShiZhuangService] redis read failed path={} ex={}", path, e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        if (!allowRemoteFallbackOnMiss) {
            throw new IllegalStateException("Config missing from Redis while shizhuang.config.allow-remote-fallback-on-miss=false: " + path);
        }

        JsonNode remote = configFeignClient.getConfigFile(path);
        if (remote == null || remote.isNull()) {
            throw new RuntimeException("Config not found: " + path);
        }

        localConfigCache.put(path, remote);
        if (redisEnabled) {
            try {
                redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(remote), redisTtlHours, TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("[ShiZhuangService] redis write failed path={} ex={}", path, e.toString());
            }
        }
        return remote;
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[ShiZhuangService] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    // Mua thời trang
    // 2. Xử lý logic mua thời trang
    @Transactional
    public void buyClothes(String playyerId, Integer clothesId, Integer num, Integer buyMoney, Integer addPayGold, Integer buyParam2) {
        if (num == null || num <= 0) throw new IllegalArgumentException("num phải > 0");
        long parsedPlayerId = parsePlayerId(playyerId);
        ClothesDTO cfg = getClothesConfig(clothesId);

        // OPTIMIZATION: Parallel currency deductions using Virtual Threads
        List<CompletableFuture<Void>> deductionFutures = new ArrayList<>();

        // Validate and deduct gold (buyMoney) - in parallel
        if (buyMoney != null && buyMoney > 0) {
            CompletableFuture<Void> goldFuture = CompletableFuture.runAsync(() -> {
                Boolean hasGold = walletFeignClient.hasEnough(playyerId, "gold", buyMoney.longValue());
                if (!Boolean.TRUE.equals(hasGold)) {
                    throw new IllegalArgumentException("Không đủ vàng để mua thời trang");
                }
                walletFeignClient.deductCurrency(Map.of(
                        "roleId", playyerId,
                        "currencyType", "gold",
                        "amount", buyMoney.longValue()
                ));
                log.info("[ShiZhuang] Deducted {} gold from player={} for clothesId={}", buyMoney, playyerId, clothesId);
            }, virtualExecutor);
            deductionFutures.add(goldFuture);
        }

        // Validate and deduct paid gold / diamonds (addPayGold) - in parallel
        if (addPayGold != null && addPayGold > 0) {
            CompletableFuture<Void> diamondFuture = CompletableFuture.runAsync(() -> {
                Boolean hasDiamond = walletFeignClient.hasEnough(playyerId, "paid_gold", addPayGold.longValue());
                if (!Boolean.TRUE.equals(hasDiamond)) {
                    throw new IllegalArgumentException("Không đủ kim cương để mua thời trang");
                }
                walletFeignClient.deductCurrency(Map.of(
                        "roleId", playyerId,
                        "currencyType", "paid_gold",
                        "amount", addPayGold.longValue()
                ));
                log.info("[ShiZhuang] Deducted {} paid_gold from player={} for clothesId={}", addPayGold, playyerId, clothesId);
            }, virtualExecutor);
            deductionFutures.add(diamondFuture);
        }

        // Wait for all parallel deductions to complete
        if (!deductionFutures.isEmpty()) {
            CompletableFuture.allOf(deductionFutures.toArray(new CompletableFuture[0])).join();
        }

        // Update vào bảng sở hữu thời trang
        PlayerClothesEntity owned = playerClothesRepository.findByPlayerIdAndClothesId(parsedPlayerId, clothesId)
                .orElse(PlayerClothesEntity.builder()
                        .playerId(parsedPlayerId)
                        .clothesId(clothesId)
                        .level(1)
                        .wearing(false)
                        .build());
        // Nếu đã có, có thể tăng level hoặc ignore (tuỳ logic game)
        playerClothesRepository.save(owned);
        log.info("[ShiZhuang] Player={} bought clothesId={} x{}", playyerId, clothesId, num);
    }


    // Mặc thời trang và cập nhật trạng thái đang đeo theo đúng type (shield/body/weapon/head)
    @Transactional
    public void wearClothes(String playerId, Integer clothesId) {
        long parsedPlayerId = parsePlayerId(playerId);
        Integer targetType = resolveClothesType(clothesId);

        // Không tự động tạo entity - phải sở hữu thời trang trước khi mặc
        PlayerClothesEntity target = playerClothesRepository.findByPlayerIdAndClothesId(parsedPlayerId, clothesId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Người chơi chưa sở hữu thời trang này: clothesId=" + clothesId));

        // Tìm tất cả clothes của player để unequip cùng type
        List<PlayerClothesEntity> owned = new ArrayList<>(playerClothesRepository.findByPlayerId(parsedPlayerId));

        for (PlayerClothesEntity entity : owned) {
            if (entity == null || entity.getClothesId() == null) {
                continue;
            }
            Integer clothesType = resolveClothesType(entity.getClothesId());
            if (targetType != null && targetType.equals(clothesType)) {
                entity.setWearing(false);
            }
        }

        target.setWearing(true);
        playerClothesRepository.saveAll(owned);
        log.info("[ShiZhuang] Player={} wore clothesId={} type={}", playerId, clothesId, targetType);
    }

    @Transactional
    public void unwearClothes(String playerId, Integer clothesId) {
        long parsedPlayerId = parsePlayerId(playerId);
        playerClothesRepository.findByPlayerIdAndClothesId(parsedPlayerId, clothesId)
                .ifPresent(entity -> {
                    entity.setWearing(false);
                    playerClothesRepository.save(entity);
                    log.info("[ShiZhuang] Player={} removed clothesId={}", playerId, clothesId);
                });
    }

    public Map<String, Integer> getCurrentAppearance(String playerId) {
        long parsedPlayerId = parsePlayerId(playerId);
        Map<String, Integer> appearance = new HashMap<>();
        // Mặc định trả về 0 (không có appearance) thay vì -1
        appearance.put("surfaceWeapon", 0);
        appearance.put("surfaceShield", 0);
        appearance.put("surfaceHead", 0);
        appearance.put("surfaceBody", 0);

        List<PlayerClothesEntity> owned = playerClothesRepository.findByPlayerId(parsedPlayerId);
        // Nếu không có clothes nào, trả về appearance mặc định (tất cả = 0)
        if (owned == null || owned.isEmpty()) {
            log.debug("[ShiZhuang] No clothes found for playerId={}, returning default appearance", playerId);
            return appearance;
        }

        Map<Integer, List<PlayerClothesEntity>> byType = new HashMap<>();
        for (PlayerClothesEntity entity : owned) {
            if (entity == null || entity.getClothesId() == null) {
                continue;
            }
            Integer clothesType = resolveClothesType(entity.getClothesId());
            if (clothesType == null) {
                continue;
            }
            byType.computeIfAbsent(clothesType, key -> new ArrayList<>()).add(entity);
        }

        applyAppearanceSlot(appearance, byType.get(0), "surfaceShield");
        applyAppearanceSlot(appearance, byType.get(1), "surfaceBody");
        applyAppearanceSlot(appearance, byType.get(2), "surfaceWeapon");
        applyAppearanceSlot(appearance, byType.get(3), "surfaceHead");
        return appearance;
    }

    // Nâng cấp thời trang
    @Transactional
    public void levelUpClothes(String playyerId, Integer clothesId) {
        long parsedPlayerId = parsePlayerId(playyerId);
        PlayerClothesEntity entity = playerClothesRepository.findByPlayerIdAndClothesId(parsedPlayerId, clothesId)
                .orElseThrow(() -> new RuntimeException("Chưa sở hữu thời trang này"));
        List<ClothesUpDTO> upList = loadClothesUpConfig();
        int nextLevel = entity.getLevel() + 1;
        ClothesUpDTO upCfg = upList.stream()
                .filter(c -> c.getClothesId().equals(clothesId) && c.getLevel() == nextLevel)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config nâng cấp cho level tiếp theo"));
        // Material check: upCfg specifies required upgrade materials
        // NOTE: If ClothesUpDTO exposes material fields (e.g., getUpItemId/getUpItemNum),
        // use ItemFeignClient to check and consume them here (see AngelService for pattern).
        log.info("[ShiZhuang] Upgrading clothesId={} to level={} for player={} (material check pending DTO fields)",
                clothesId, nextLevel, playyerId);
        entity.setLevel(nextLevel);
        playerClothesRepository.save(entity);
    }

    // Lấy danh sách thời trang sở hữu
    public List<PlayerClothesDTO> getClothes(String playyerId) {
        List<PlayerClothesEntity> entities = playerClothesRepository.findByPlayerId(parsePlayerId(playyerId));
        return entities.stream()
                .map(PlayerClothesMapper::toDTO)
                .collect(Collectors.toList());
    }

    private void applyAppearanceSlot(Map<String, Integer> appearance, List<PlayerClothesEntity> entries, String fieldName) {
        PlayerClothesEntity selected = pickDisplayedClothes(entries);
        if (selected != null && selected.getClothesId() != null) {
            appearance.put(fieldName, selected.getClothesId());
        }
    }

    private PlayerClothesEntity pickDisplayedClothes(List<PlayerClothesEntity> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparingInt((PlayerClothesEntity entity) -> Boolean.TRUE.equals(entity.getWearing()) ? 1 : 0)
                        .thenComparingInt(entity -> entity.getLevel() != null ? entity.getLevel() : 0)
                        .thenComparingLong(entity -> entity.getId() != null ? entity.getId() : 0L))
                .orElse(null);
    }

    private Integer resolveClothesType(Integer clothesId) {
        if (clothesId == null || clothesId <= 0) {
            return null;
        }
        try {
            ClothesDTO cfg = getClothesConfig(clothesId);
            return cfg != null ? cfg.getClothesType() : null;
        } catch (Exception e) {
            log.debug("[ShiZhuang] resolveClothesType skipped clothesId={} ex={}", clothesId, e.toString());
            return null;
        }
    }

    private long parsePlayerId(String playerId) {
        try {
            return Long.parseLong(playerId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("roleId/playerId không hợp lệ: " + playerId, ex);
        }
    }


    public ShiZhuangDto get(String userId, int id) {
        ShiZhuangEntity cached = cache.getFromCache(userId, id);
        if (cached != null) return toDto(cached);

        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(Long.parseLong(userId), id);
        if (entity.isPresent()) {
            cache.putToCache(entity.get());
            return toDto(entity.get());
        }
        return null;
    }

    public List<ShiZhuangDto> getAll(String userId) {
        return repo.findByUserId(Long.parseLong(userId)).stream().map(this::toDto).collect(Collectors.toList());
    }

    public ShiZhuangDto addOrUpdate(ShiZhuangDto dto) {
        ShiZhuangEntity entity = repo.findByUserIdAndId(Long.parseLong(dto.getUserId()), dto.getId() != 0 ? dto.getId() : -1)
                .orElse(ShiZhuangEntity.builder().userId(Long.parseLong(dto.getUserId())).level(dto.getLevel()).build());
        entity.setLevel(dto.getLevel());
        ShiZhuangEntity saved = repo.save(entity);
        cache.putToCache(saved);

        // Produce Kafka event khi thêm hoặc update

        return toDto(saved);
    }

    public boolean delete(String userId, int id) {
        Optional<ShiZhuangEntity> entity = repo.findByUserIdAndId(Long.parseLong(userId), id);
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
                .userId(entity.getUserId() != null ? String.valueOf(entity.getUserId()) : null)
                .level(entity.getLevel())
                .build();
    }
}
