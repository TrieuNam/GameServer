package com.SouthMillion.shop_service.service;

import com.SouthMillion.shop_service.config.ShopConfigCache;
import com.SouthMillion.shop_service.entity.ShopLimit;
import com.SouthMillion.shop_service.repository.ShopLimitRepository;
import com.SouthMillion.shop_service.service.config.BagFeign;
import com.SouthMillion.shop_service.service.config.ItemMetaFeign;
import com.SouthMillion.shop_service.service.config.RoleFeign;
import com.SouthMillion.shop_service.service.config.WalletFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagOkResp;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopConfigCache cfg;
    private final BagFeign bagFeign;
    private final ShopLimitRepository limitRepo;
    private final WalletFeignClient walletFeign;
    private final ItemMetaFeign itemMeta;

    // Virtual Thread executor for parallel operations
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // optional: có thể tắt dependency này ở test
    @Autowired(required = false) @Nullable
    private RoleFeign roleFeign;

    @Value("${app.shenmi.default-slots:6}")
    private int shenmiSlots;

    @Value("${app.shenmi.timezone:UTC}")
    private String tz;

    // ===================== Helpers =====================
    private static int asInt(JsonNode n, String field, int def) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull()) return def;
        if (x.isNumber()) return x.intValue();
        try { return Integer.parseInt(x.asText()); } catch (Exception e) { return def; }
    }

    private static long asLong(JsonNode n, String field, long def) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull()) return def;
        if (x.isNumber()) return x.longValue();
        try { return Long.parseLong(x.asText()); } catch (Exception e) { return def; }
    }

    private static String asText(JsonNode n, String field, String def) {
        JsonNode x = n.get(field);
        return (x == null || x.isNull()) ? def : x.asText();
    }

    private static int asInt(Object value, int def) {
        if (value instanceof Number num) return num.intValue();
        try { return value == null ? def : Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return def; }
    }

    private static long ceilMul(long base, double discount) {
        double d = discount;
        if (d > 1.0 && d <= 100.0) d = d / 100.0; // hỗ trợ 80 -> 0.8
        return (long) Math.ceil(base * d);
    }

    private ZoneId zoneId() { return ZoneId.of(tz); }

    private long nextMidnightEpochSec() {
        ZonedDateTime now = ZonedDateTime.now(zoneId());
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId());
        return nextMidnight.toEpochSecond();
    }

    private int resolveLevel(String roleId, Integer levelMaybe) {
        if (levelMaybe != null && levelMaybe > 0) return levelMaybe;
        try {
            if (roleFeign != null) {
                RoleDTOs.RoleResp resp = roleFeign.detail(Long.parseLong(roleId)).orElse(null);
                int lv = extractLevel(resp);
                if (lv > 0) return lv;
            }
        } catch (Exception ignore) {}
        return 1; // fallback
    }

    /**
     * Chỉnh hàm này theo cấu trúc RoleDTOs.RoleResp thực tế của bạn.
     * Ở đây giả định RoleResp có getter getLevel().
     */
    private int extractLevel(RoleDTOs.RoleResp resp) {
        if (resp == null) return 0;
        try {
            // nếu RoleResp là class thường có getLevel():
            return resp.getLevel();
        } catch (Throwable ignore) {
        }
        // nếu RoleResp bọc data (ví dụ getData().getLevel()), hãy đổi ở đây:
        // try { return resp.getData().getLevel(); } catch (Throwable ignore) {}
        return 0;
    }

    @org.springframework.cache.annotation.Cacheable(value = "shopItemMeta", key = "#itemId", unless = "#result == null")
    private Map<String, Object> getItemMeta(long itemId) {
        if (itemId <= 0) return null;
        Map<String, Map<String, Object>> metas = itemMeta.batchMeta(String.valueOf(itemId));
        return metas == null ? null : metas.get(String.valueOf(itemId));
    }

    private boolean isVirtual(long itemId) {
        if (itemId <= 0) return false;
        Map<String, Object> m = getItemMeta(itemId);
        if (m == null) return false;
        Object v = m.get("isVirtual");
        if (v instanceof Number num) return num.intValue() == 1;
        try { return Integer.parseInt(String.valueOf(v)) == 1; } catch (Exception ignore) { return false; }
    }

    private int resolveItemQuality(Map<String, Object> meta) {
        if (meta == null) return 1;
        return Math.max(1, asInt(meta.getOrDefault("quality", meta.getOrDefault("color", meta.get("q"))), 1));
    }

    // ===================== INFO (bootstrap) =====================
    public ResultDTO<ShopDTOs.InfoResp> info(String roleId, Integer levelMaybe, Integer slotsOverride) {
        int level = resolveLevel(roleId, levelMaybe);
        ResultDTO<ShopDTOs.ShopListResp> mystery = listMystery(level, slotsOverride);
        ShopDTOs.InfoResp out = ShopDTOs.InfoResp.builder()
                .mysterySlots((slotsOverride != null && slotsOverride > 0) ? slotsOverride : shenmiSlots)
                .nextResetEpoch(nextMidnightEpochSec())
                .mystery(mystery.data())
                .build();
        return ResultDTO.ok(out);
    }

    // ===================== LIST COMMON =====================
    public ResultDTO<ShopDTOs.ShopListResp> listCommon(ShopDTOs.ListCommonReq req) {
        JsonNode root = cfg.common();
        Iterable<JsonNode> arr = root.isArray() ? root : root.withArray("list");

        List<ShopDTOs.ShopItem> items = StreamSupport.stream(arr.spliterator(), false)
                .filter(n -> asInt(n, "page", -1) == req.page())
                // chỉ lọc shopType nếu config có trường nhận diện
                .filter(n -> {
                    boolean hasShopType = n.hasNonNull("page_1") || n.hasNonNull("shop_type");
                    if (!hasShopType) return true;
                    int v = asInt(n, "page_1", asInt(n, "shop_type", -9999));
                    return v == req.shopType();
                })
                .filter(n -> {
                    int lvMin = asInt(n, "level_min", 0);
                    int lvMax = asInt(n, "level_max", Integer.MAX_VALUE);
                    int lv = req.level();
                    return lv >= lvMin && lv <= lvMax;
                })
                .map(n -> {
                    int index = asInt(n, "index", asInt(n, "id", -1));
                    long priceItemId = asLong(n, "exchange_item_id", asLong(n, "buy_item", 0));
                    long priceNum = asLong(n, "exchange_item_num", asLong(n, "buy_item_num", 0));
                    long rewardItemId = asLong(n, "reward_item_id", asLong(n, "item_id", 0));
                    long rewardNum = asLong(n, "reward_num", asLong(n, "num", 0));
                    String name = asText(n, "name", "item-" + rewardItemId);
                    int lvMin = asInt(n, "level_min", 0);
                    int lvMax = asInt(n, "level_max", 9999);
                    return new ShopDTOs.ShopItem(
                            String.valueOf(index), name,
                            priceItemId, priceNum,
                            rewardItemId, rewardNum,
                            lvMin, lvMax
                    );
                })
                .collect(Collectors.toList());

        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===================== LIST CLOTH =====================
    public ResultDTO<ShopDTOs.ShopListResp> listCloth(ShopDTOs.ListClothReq req) {
        JsonNode root = cfg.cloth();
        Iterable<JsonNode> arr = root.isArray() ? root : root.withArray("list");

        List<ShopDTOs.ShopItem> items = StreamSupport.stream(arr.spliterator(), false)
                .filter(n -> asInt(n, "shop_type", -1) == req.page())
                .filter(n -> {
                    int lvMin = asInt(n, "level_min", 0);
                    int lvMax = asInt(n, "level_max", Integer.MAX_VALUE);
                    int lv = req.level();
                    return lv >= lvMin && lv <= lvMax;
                })
                .map(n -> {
                    int seq = asInt(n, "seq", asInt(n, "index", -1));
                    long basePriceNum = asLong(n, "price", asLong(n, "buy_item_num", 0));
                    double discount = n.hasNonNull("discount") ? n.get("discount").asDouble(1.0) : 1.0;
                    long finalPrice = ceilMul(basePriceNum, discount);
                    long priceItemId = asLong(n, "buy_item", asLong(n, "exchange_item_id", 0));
                    long rewardItemId = asLong(n, "item_id", 0);
                    long rewardNum = asLong(n, "num", 1);
                    String name = asText(n, "name", "cloth-" + rewardItemId);
                    int lvMin = asInt(n, "level_min", 0);
                    int lvMax = asInt(n, "level_max", 9999);
                    return new ShopDTOs.ShopItem(
                            String.valueOf(seq), name,
                            priceItemId, finalPrice,
                            rewardItemId, rewardNum,
                            lvMin, lvMax
                    );
                })
                .collect(Collectors.toList());

        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===================== LIST SHENMI =====================
    public ResultDTO<ShopDTOs.ShopListResp> listMystery(int level, Integer slotsOverride) {
        int slots = (slotsOverride != null && slotsOverride > 0) ? slotsOverride : shenmiSlots;
        JsonNode root = cfg.shenmi();
        JsonNode arr = root.isArray() ? root : root.withArray("shop");

        List<JsonNode> candidates = new ArrayList<>();
        for (JsonNode n : arr) {
            int lvMin = asInt(n, "level_min", 0);
            int lvMax = asInt(n, "level_max", 9999);
            if (level >= lvMin && level <= lvMax) candidates.add(n);
        }

        Collections.shuffle(candidates, new SecureRandom());
        List<JsonNode> pick = candidates.stream().limit(slots).toList();

        List<ShopDTOs.ShopItem> items = pick.stream().map(n -> {
            int index = asInt(n, "index", asInt(n, "id", -1));
            long priceItemId = asLong(n, "buy_item", asLong(n, "exchange_item_id", 0));
            long priceNum = asLong(n, "buy_item_num", asLong(n, "exchange_item_num", 0));
            long rewardItemId = asLong(n, "item_id", 0);
            long rewardNum = asLong(n, "num", 1);
            String name = asText(n, "name", "mystery-" + rewardItemId);
            int lvMin = asInt(n, "level_min", 0);
            int lvMax = asInt(n, "level_max", 9999);
            return new ShopDTOs.ShopItem(
                    String.valueOf(index), name,
                    priceItemId, priceNum,
                    rewardItemId, rewardNum,
                    lvMin, lvMax
            );
        }).toList();

        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===================== REFRESH MYSTERY (SHENMI) =====================
    public ResultDTO<ShopDTOs.ShopListResp> refreshMystery(String roleId, int shardId, int slot) {
        // Resolve player level
        int level = resolveLevel(roleId, null);
        
        JsonNode root = cfg.shenmi();
        JsonNode arr = root.isArray() ? root : root.withArray("shop");

        List<JsonNode> candidates = new ArrayList<>();
        for (JsonNode n : arr) {
            int lvMin = asInt(n, "level_min", 0);
            int lvMax = asInt(n, "level_max", 9999);
            if (level >= lvMin && level <= lvMax) candidates.add(n);
        }

        Collections.shuffle(candidates, new SecureRandom());
        List<JsonNode> pick = candidates.stream().limit(1).toList();

        List<ShopDTOs.ShopItem> items = pick.stream().map(n -> {
            int index = asInt(n, "index", asInt(n, "id", -1));
            long priceItemId = asLong(n, "buy_item", asLong(n, "exchange_item_id", 0));
            long priceNum = asLong(n, "buy_item_num", asLong(n, "exchange_item_num", 0));
            long rewardItemId = asLong(n, "item_id", 0);
            long rewardNum = asLong(n, "num", 1);
            String name = asText(n, "name", "mystery-" + rewardItemId);
            int lvMin = asInt(n, "level_min", 0);
            int lvMax = asInt(n, "level_max", 9999);
            return new ShopDTOs.ShopItem(
                    String.valueOf(index), name,
                    priceItemId, priceNum,
                    rewardItemId, rewardNum,
                    lvMin, lvMax
            );
        }).toList();

        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===================== BUY =====================
    @Transactional
    public ResultDTO<ShopDTOs.BuyResp> buy(ShopDTOs.BuyReq req) {
        // 1) find config
        CfgLine rec = findConfigRecord(req);
        if (rec == null) return ResultDTO.fail("CONFIG_NOT_FOUND");

        // 2) quota check (0: none, 1: daily, 2: forever)
        int quotaType = rec.quotaType;
        int quotaParam = rec.quotaParam;
        if (quotaType != 0 && quotaParam > 0) {
            String dayStr = (quotaType == 1)
                    ? LocalDate.now(zoneId()).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    : "ALL";
            String period = (quotaType == 1) ? "DAILY" : "FOREVER";

            ShopLimit lim = limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    Long.parseLong(req.roleId()), req.kind().name(), req.indexOrSeq(), period, dayStr
            ).orElse(null);

            long already = (lim == null) ? 0 : lim.getCount();
            long after = already + req.num();
            if (after > quotaParam) return ResultDTO.fail("QUOTA_EXCEEDED");
        }

        // 3) consume cost
        long costItemId = rec.priceItemId;
        long costNum = rec.priceNum * req.num();
        if (costItemId > 0 && costNum > 0) {
            if (isVirtual(costItemId)) {
                WalletDTOs.BatchReq costReq = WalletDTOs.BatchReq.builder()
                        .roleId(req.roleId())
                        .changes(List.of(WalletDTOs.Change.builder()
                                .itemId(costItemId)
                                .amount(costNum)
                                .build()))
                        .reason(101)      // SHOP_BUY
                        .reasonType(1)
                        .build();
                // Giả định walletFeign trả về ResultDTO<WalletDTOs.MutateResp>
                var wr = walletFeign.batchCost(costReq);
                if (wr == null || wr.getCode() != 0 || wr.getData() == null || !wr.getData().ok()) {
                    String err = (wr == null) ? "WALLET_COST_NULL" :
                            (wr.getData() != null && wr.getData().error() != null) ? wr.getData().error() : wr.getMessage();
                    return ResultDTO.fail(err != null ? err : "WALLET_COST_FAIL");
                }
            } else {
                BagConsumeReq consumeReq = BagConsumeReq.builder()
                        .userId(1L) // audit field - roleId used for bag operations
                        .roleId(Long.parseLong(req.roleId()))
                        .itemId((int) costItemId)
                        .amount((int) costNum)
                        .source("SHOP_BUY")
                        .build();
                BagOkResp c = bagFeign.consume(consumeReq);
                if (c == null || !c.isOk()) return ResultDTO.fail(c == null ? "CONSUME_FAIL" : c.getMessage());
            }
        }

        // 4) add reward
        long rewardItemId = rec.rewardItemId;
        long rewardNum = rec.rewardNum * req.num();
        if (rewardItemId > 0 && rewardNum > 0) {
            if (isVirtual(rewardItemId)) {
                WalletDTOs.BatchReq addReq = WalletDTOs.BatchReq.builder()
                        .roleId(req.roleId())
                        .changes(List.of(WalletDTOs.Change.builder()
                                .itemId(rewardItemId)
                                .amount(rewardNum)
                                .build()))
                        .reason(102)      // SHOP_REWARD
                        .reasonType(1)
                        .build();
                var wr = walletFeign.batchAdd(addReq);
                if (wr == null || wr.getCode() != 0 || wr.getData() == null || !wr.getData().ok()) {
                    String err = (wr == null) ? "WALLET_ADD_NULL" :
                            (wr.getData() != null && wr.getData().error() != null) ? wr.getData().error() : wr.getMessage();
                    return ResultDTO.fail(err != null ? err : "WALLET_ADD_FAIL");
                }
            } else {
                Map<String, Object> rewardMeta = getItemMeta(rewardItemId);
                var item = BagAddItemReq.Item.builder()
                        .itemId((int) rewardItemId)
                        .amount((int) rewardNum)
                    .quality(resolveItemQuality(rewardMeta))
                    .bagType(req.receiveBagType())
                        .bound(false)
                        .build();
                BagAddItemReq addReq = BagAddItemReq.builder()
                        .userId(1L) // audit field - roleId used for bag operations
                        .roleId(Long.parseLong(req.roleId()))
                        .items(List.of(item))
                        .source("SHOP_REWARD")
                        .build();
                var a = bagFeign.add(addReq);
                if (a == null || !a.isSuccess()) return ResultDTO.fail(a == null ? "ADD_FAIL" : a.getMessage());
            }
        }

        // 5) persist quota
        if (quotaType != 0 && quotaParam > 0) {
            String dayStr = (quotaType == 1)
                    ? LocalDate.now(zoneId()).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    : "ALL";
            String period = (quotaType == 1) ? "DAILY" : "FOREVER";

            ShopLimit lim = limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    Long.parseLong(req.roleId()), req.kind().name(), req.indexOrSeq(), period, dayStr
            ).orElse(null);
            if (lim == null) {
                lim = new ShopLimit();
                lim.setRoleId(Long.parseLong(req.roleId()));
                lim.setKind(req.kind().name());
                lim.setEntryIndex(req.indexOrSeq());
                lim.setPeriod(period);
                lim.setDayStr(dayStr);
                lim.setCount(0);
            }
            lim.setCount(lim.getCount() + req.num());
            limitRepo.save(lim);
        }

        // Build success response with cost and reward info
        // Query remaining balance for the currency used to pay (virtual items only)
        long remaining = 0;
        if (costItemId > 0 && isVirtual(costItemId)) {
            try {
                var balResp = walletFeign.get(req.roleId(), java.util.List.of(costItemId));
                if (balResp != null && balResp.getCode() == 0 && balResp.getData() != null
                        && balResp.getData().balances() != null) {
                    Long bal = balResp.getData().balances().get(costItemId);
                    if (bal != null) remaining = bal;
                }
            } catch (Exception ignored) {
                log.warn("Failed to query remaining balance for roleId={} itemId={}", req.roleId(), costItemId);
            }
        }
        ShopDTOs.ShopItem rewardItem = new ShopDTOs.ShopItem(
            String.valueOf(rec.rewardItemId),
            "", // name not needed in response
            rec.priceItemId,
            rec.priceNum,
            rec.rewardItemId,
            rec.rewardNum * req.num(),
            0, 0 // level range not needed
        );
        return ResultDTO.ok(ShopDTOs.BuyResp.builder()
                .ok(true)
                .cost(costNum)
                .remainingBalance(remaining)
                .items(List.of(rewardItem))
                .build());
    }

    // ===================== Config lookup =====================
    private CfgLine findConfigRecord(ShopDTOs.BuyReq req) {
        return switch (req.kind()) {
            case COMMON -> findFrom(cfg.common(), "index", req.indexOrSeq(), /*cloth*/ false);
            case CLOTH  -> findFrom(cfg.cloth(),  "seq",   req.indexOrSeq(), /*cloth*/ true);
            case SHENMI -> findFrom(cfg.shenmi(), "index", req.indexOrSeq(), /*cloth*/ false);
        };
    }

    private CfgLine findFrom(JsonNode root, String key, int value, boolean isCloth) {
        Iterable<JsonNode> arr = root.isArray() ? root : root.withArray("list");
        for (JsonNode n : arr) {
            if (asInt(n, key, -9999) == value) {
                long priceItemId = asLong(n, "exchange_item_id", asLong(n, "buy_item", 0));
                long priceNum    = asLong(n, "exchange_item_num", asLong(n, "buy_item_num", 0));
                if (isCloth) {
                    long base = priceNum;
                    double discount = n.hasNonNull("discount") ? n.get("discount").asDouble(1.0) : 1.0;
                    priceNum = ceilMul(base, discount);
                }
                long rewardItemId = asLong(n, "reward_item_id", asLong(n, "item_id", 0));
                long rewardNum    = asLong(n, "reward_num", asLong(n, "num", 1));
                int quotaType     = asInt(n, "quota_type", 0); // 0=none,1=daily,2=forever
                int quotaParam    = asInt(n, "param", 0);      // max buy count
                return new CfgLine(priceItemId, priceNum, rewardItemId, rewardNum, quotaType, quotaParam);
            }
        }
        return null;
    }

    private record CfgLine(long priceItemId, long priceNum, long rewardItemId, long rewardNum,
                           int quotaType, int quotaParam) {}
}