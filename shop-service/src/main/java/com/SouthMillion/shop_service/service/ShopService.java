package com.SouthMillion.shop_service.service;

import com.SouthMillion.shop_service.config.ShopConfigCache;
import com.SouthMillion.shop_service.entity.ShopLimit;
import com.SouthMillion.shop_service.repository.ShopLimitRepository;
import com.SouthMillion.shop_service.service.config.BagFeign;
import com.SouthMillion.shop_service.service.config.ItemMetaFeign;
import com.SouthMillion.shop_service.service.config.WalletFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagOkResp;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopConfigCache cfg;
    private final BagFeign bagFeign;
    private final ShopLimitRepository limitRepo;
    private final WalletFeignClient walletFeign;

    private final ItemMetaFeign itemMeta;

    @Value("${app.shenmi.default-slots:6}")
    private int shenmiSlots;

    @Value("${app.shenmi.timezone:UTC}")
    private String tz;

    // ===== Helpers =====
    private static int asInt(JsonNode n, String field, int def) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull()) return def;
        if (x.isNumber()) return x.intValue();
        try {
            return Integer.parseInt(x.asText());
        } catch (Exception e) {
            return def;
        }
    }

    private static long asLong(JsonNode n, String field, long def) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull()) return def;
        if (x.isNumber()) return x.longValue();
        try {
            return Long.parseLong(x.asText());
        } catch (Exception e) {
            return def;
        }
    }

    private static String asText(JsonNode n, String field, String def) {
        JsonNode x = n.get(field);
        return (x == null || x.isNull()) ? def : x.asText();
    }

    private static long ceilMul(long base, double discount) {
        double d = discount;
        if (d > 1.0 && d <= 100.0) d = d / 100.0; // nếu file dùng % (80 = 80%)
        return (long) Math.ceil(base * d);
    }

    // ===== List COMMON =====
    public ResultDTO<ShopDTOs.ShopListResp> listCommon(ShopDTOs.ListCommonReq req) {
        JsonNode root = cfg.common();
        // TODO: tuỳ JSON, có thể là array root hoặc object {list:[...]}
        Iterable<JsonNode> arr = root.isArray() ? root : root.withArray("list");
        List<ShopDTOs.ShopItem> items = StreamSupport.stream(arr.spliterator(), false)
                .filter(n -> asInt(n, "page", -1) == req.page())
                .filter(n -> asInt(n, "page_1", -1) == req.shopType()) // nếu không có, bạn bỏ filter này
                .filter(n -> {
                    int lvMin = asInt(n, "level_min", 0);
                    int lvMax = asInt(n, "level_max", Integer.MAX_VALUE);
                    int lv = req.level();
                    return lv >= lvMin && lv <= lvMax;
                })
                .map(n -> {
                    // Các field gợi ý (đổi theo JSON thực tế)
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

    // ===== List CLOTH =====
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
                }).collect(Collectors.toList());
        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===== List SHENMI (mystery) – chọn ngẫu nhiên =====
    public ResultDTO<ShopDTOs.ShopListResp> listMystery(int level, Integer slotsOverride) {
        int slots = (slotsOverride != null && slotsOverride > 0) ? slotsOverride : shenmiSlots;
        JsonNode root = cfg.shenmi();

        JsonNode arr = root.isArray() ? root : root.withArray("list");
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
            return new ShopDTOs.ShopItem(String.valueOf(index), name,
                    priceItemId, priceNum,
                    rewardItemId, rewardNum,
                    lvMin, lvMax);
        }).toList();

        return ResultDTO.ok(new ShopDTOs.ShopListResp(items));
    }

    // ===== BUY =====
    @Transactional
    public ResultDTO<ShopDTOs.BuyResp> buy(ShopDTOs.BuyReq req) {
        // 1) tìm config
        CfgLine rec = findConfigRecord(req);
        if (rec == null) return ResultDTO.fail("CONFIG_NOT_FOUND");

        // 2) quota
        int quotaType = rec.quotaType;
        int quotaParam = rec.quotaParam;
        if (quotaType != 0 && quotaParam > 0) {
            String dayStr = (quotaType == 1)
                    ? LocalDate.now(ZoneId.of(tz)).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    : "ALL";
            String period = (quotaType == 1) ? "DAILY" : "FOREVER";

            ShopLimit lim = limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    req.roleId(), req.kind().name(), req.indexOrSeq(), period, dayStr
            ).orElse(null);

            long already = (lim == null) ? 0 : lim.getCount();
            long after = already + req.num();
            if (after > quotaParam) return ResultDTO.fail("QUOTA_EXCEEDED");
        }

        // 3) consume giá (tùy virtual hay không)
        long costItemId = rec.priceItemId;
        long costNum = rec.priceNum * req.num();
        if (costItemId > 0 && costNum > 0) {
            if (isVirtual(costItemId)) {
                // WALLET COST
                WalletDTOs.BatchReq costReq = new WalletDTOs.BatchReq(
                        req.roleId(),
                        List.of(new WalletDTOs.Change(costItemId, costNum)),
                        null,          // idemKey: nếu có req.txId() hoặc orderId, truyền vào đây
                        101, 1         // reason, reasonType: SHOP_BUY
                );
                var wr = walletFeign.batchCost(costReq);
                if (wr == null || wr.getCode() != 0 || wr.getData() == null || !wr.getData().ok()) {
                    String err = (wr == null) ? "WALLET_COST_NULL" :
                            (wr.getData() != null && wr.getData().error() != null) ? wr.getData().error() : wr.getMessage();
                    return ResultDTO.fail(err != null ? err : "WALLET_COST_FAIL");
                }
            } else {
                // BAG CONSUME (vật phẩm vật lý dùng làm giá)
                BagConsumeReq consumeReq = new BagConsumeReq(
                        req.roleId(),
                        req.walletBagType(),
                        List.of(new BagConsumeReq.Cost(costItemId, costNum))
                );
                BagOkResp c = bagFeign.consume(consumeReq);
                if (c == null || !c.ok()) return ResultDTO.fail(c == null ? "CONSUME_FAIL" : c.error());
            }
        }

        // 4) add reward (tùy virtual hay không)
        long rewardItemId = rec.rewardItemId;
        long rewardNum = rec.rewardNum * req.num();
        if (rewardItemId > 0 && rewardNum > 0) {
            if (isVirtual(rewardItemId)) {
                WalletDTOs.BatchReq addReq = new WalletDTOs.BatchReq(
                        req.roleId(),
                        List.of(new WalletDTOs.Change(rewardItemId, rewardNum)),
                        null,          // idemKey nếu có
                        102, 1         // reason: SHOP_REWARD
                );
                var wr = walletFeign.batchAdd(addReq);
                if (wr == null || wr.getCode() != 0 || wr.getData() == null || !wr.getData().ok()) {
                    String err = (wr == null) ? "WALLET_ADD_NULL" :
                            (wr.getData() != null && wr.getData().error() != null) ? wr.getData().error() : wr.getMessage();
                    return ResultDTO.fail(err != null ? err : "WALLET_ADD_FAIL");
                }
            } else {
                BagAddItemReq addReq = new BagAddItemReq(
                        req.roleId(),
                        req.receiveBagType(),
                        List.of(new BagAddItemReq.Item(rewardItemId, rewardNum))
                );
                var a = bagFeign.add(addReq);
                if (a == null || !a.ok()) return ResultDTO.fail(a == null ? "ADD_FAIL" : a.error());
            }
        }

        // 5) ghi hạn mức
        if (quotaType != 0 && quotaParam > 0) {
            String dayStr = (quotaType == 1)
                    ? LocalDate.now(ZoneId.of(tz)).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    : "ALL";
            String period = (quotaType == 1) ? "DAILY" : "FOREVER";

            ShopLimit lim = limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    req.roleId(), req.kind().name(), req.indexOrSeq(), period, dayStr
            ).orElse(null);
            if (lim == null) {
                lim = new ShopLimit();
                lim.setRoleId(req.roleId());
                lim.setKind(req.kind().name());
                lim.setEntryIndex(req.indexOrSeq());
                lim.setPeriod(period);
                lim.setDayStr(dayStr);
                lim.setCount(0);
            }
            lim.setCount(lim.getCount() + req.num());
            limitRepo.save(lim);
        }

        return ResultDTO.ok(new ShopDTOs.BuyResp(true, null));
    }

    // NEW: kiểm tra 1 item có phải virtual không (dựa vào item-service)
    private boolean isVirtual(long itemId) {
        if (itemId <= 0) return false;
        Map<String, Map<String, Object>> metas = itemMeta.batchMeta(String.valueOf(itemId));
        Map<String, Object> m = metas.get(String.valueOf(itemId));
        if (m == null) return false;
        Object v = m.get("isVirtual");
        if (v instanceof Number num) return num.intValue() == 1;
        try {
            return Integer.parseInt(String.valueOf(v)) == 1;
        } catch (Exception ignore) {
            return false;
        }
    }


    // ======= Tìm config record theo kind + index/seq =======
    private CfgLine findConfigRecord(ShopDTOs.BuyReq req) {
        return switch (req.kind()) {
            case COMMON -> findFrom(cfg.common(), "index", req.indexOrSeq(), /*isCloth*/ false);
            case CLOTH -> findFrom(cfg.cloth(), "seq", req.indexOrSeq(), /*isCloth*/ true);
            case SHENMI -> findFrom(cfg.shenmi(), "index", req.indexOrSeq(), /*isCloth*/ false);
        };
    }

    private CfgLine findFrom(JsonNode root, String key, int value, boolean isCloth) {
        Iterable<JsonNode> arr = root.isArray() ? root : root.withArray("list");
        for (JsonNode n : arr) {
            if (asInt(n, key, -9999) == value) {
                long priceItemId = asLong(n, "exchange_item_id", asLong(n, "buy_item", 0));
                long priceNum = asLong(n, "exchange_item_num", asLong(n, "buy_item_num", 0));
                if (isCloth) {
                    long base = priceNum;
                    double discount = n.hasNonNull("discount") ? n.get("discount").asDouble(1.0) : 1.0;
                    priceNum = ceilMul(base, discount);
                }
                long rewardItemId = asLong(n, "reward_item_id", asLong(n, "item_id", 0));
                long rewardNum = asLong(n, "reward_num", asLong(n, "num", 1));
                int quotaType = asInt(n, "quota_type", 0);
                int quotaParam = asInt(n, "param", 0);
                return new CfgLine(priceItemId, priceNum, rewardItemId, rewardNum, quotaType, quotaParam);
            }
        }
        return null;
    }

    private record CfgLine(long priceItemId, long priceNum, long rewardItemId, long rewardNum,
                           int quotaType, int quotaParam) {
    }
}