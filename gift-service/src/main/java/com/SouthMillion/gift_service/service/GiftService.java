package com.SouthMillion.gift_service.service;

import com.SouthMillion.gift_service.config.GiftConfigCache;
import com.SouthMillion.gift_service.service.client.BagInternalFeign;
import com.SouthMillion.gift_service.service.client.ItemMetaFeign;
import com.SouthMillion.gift_service.service.client.WalletFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.gift.GiftDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftConfigCache cfg;
    private final ItemMetaFeign itemMetaFeign;
    private final BagInternalFeign bagFeign;
    private final WalletFeignClient walletFeign;

    @Value("${app.gift.default-bag-type:1}")
    private int defaultBagType;

    private final Random rnd = new SecureRandom();

    public GiftDTOs.GiftInfoResp info(long giftItemId) {
        var map = cfg.compile();
        var box = map.get(giftItemId);
        if (box == null) {
            return GiftDTOs.GiftInfoResp.builder()
                    .giftItemId(giftItemId)
                    .giftType(0).randNum(0).pool(List.of()).build();
        }
        var pool = box.getItems().stream()
                .map(x -> new GiftDTOs.RollItem(x.getItemId(), x.getCount()))
                .collect(Collectors.toList());
        return GiftDTOs.GiftInfoResp.builder()
                .giftItemId(giftItemId)
                .giftType(box.getGiftType())
                .randNum(box.getRandNum())
                .pool(pool).build();
    }

    public GiftDTOs.OpenResp open(GiftDTOs.OpenReq req) {
        int bagType = req.getBagType() > 0 ? req.getBagType() : defaultBagType;

        var compiled = cfg.compile();
        var box = compiled.get(req.getGiftItemId());
        if (box == null) {
            return GiftDTOs.OpenResp.builder().ok(false).error("GIFT_NOT_FOUND").consumed(0).build();
        }

        // Roll items for N times
        Map<Long, Long> gain = new HashMap<>();
        if (box.getGiftType() == 1) {
            // DefGift: coi như cho toàn bộ list, lặp count lần
            for (int i = 0; i < req.getCount(); i++) {
                for (var gi : box.getItems()) {
                    gain.merge(gi.getItemId(), gi.getCount(), Long::sum);
                }
            }
        } else {
            // RandGift: mỗi hộp rút randNum lần, mỗi lần 1 item theo trọng số rate
            int totalWeight = box.getItems().stream().mapToInt(GiftConfigCache.GiftItem::getRate).sum();
            if (totalWeight <= 0) totalWeight = box.getItems().size();
            for (int t = 0; t < req.getCount(); t++) {
                for (int r = 0; r < Math.max(1, box.getRandNum()); r++) {
                    int v = rnd.nextInt(totalWeight) + 1;
                    int acc = 0;
                    for (var gi : box.getItems()) {
                        acc += Math.max(1, gi.getRate());
                        if (v <= acc) {
                            gain.merge(gi.getItemId(), gi.getCount(), Long::sum);
                            break;
                        }
                    }
                }
            }
        }

        // Lấy meta để tách virtual (ví) / normal (túi)
        Set<Long> itemIds = new HashSet<>(gain.keySet());
        itemIds.add(req.getGiftItemId()); // để phòng cần check thêm
        String csv = itemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<String, Map<String, Object>> meta = itemMetaFeign.batchMeta(csv);
        if (meta == null) meta = Map.of();

        List<BagAddItemReq.Item> bagAdds = new ArrayList<>();
        List<WalletDTOs.Change> walletAdds = new ArrayList<>();

        for (var e : gain.entrySet()) {
            long id = e.getKey();
            long cnt = e.getValue();
            var m = meta.get(String.valueOf(id));
            int isVirtual = (m == null) ? 0 : ((Number) m.getOrDefault("isVirtual", 0)).intValue();
            if (isVirtual == 1) {
                walletAdds.add(new WalletDTOs.Change(id, cnt));
            } else {
                bagAdds.add(new BagAddItemReq.Item(id, cnt));
            }
        }

        // 1) Consume hộp quà ở túi
        var consume = new BagConsumeReq(req.getRoleId(), bagType,
                List.of(new BagConsumeReq.Cost(req.getGiftItemId(), req.getCount())));
        var cResp = bagFeign.consume(consume);
        if (cResp == null || !cResp.ok()) {
            String err = (cResp == null) ? "BAG_CONSUME_FAIL" : cResp.error();
            return GiftDTOs.OpenResp.builder().ok(false).error(err).consumed(0).build();
        }

        // 2) Cộng ví (nếu có)
        Map<Long, Long> walletChanged = Map.of();
        if (!walletAdds.isEmpty()) {
            var wReq = new WalletDTOs.BatchReq(req.getRoleId(), walletAdds);
            var wResp = walletFeign.batchAdd(wReq);
            if (wResp == null || wResp.getCode() != 0 || wResp.getData() == null || !wResp.getData().ok()) {
                String err = (wResp == null) ? "WALLET_ADD_FAIL" :
                        (wResp.getMessage() != null ? wResp.getMessage() : "WALLET_ADD_FAIL");
                return GiftDTOs.OpenResp.builder().ok(false).error(err).consumed(0).build();
            }
            walletChanged = wResp.getData().newBalances();
        }

        // 3) Cộng túi (nếu có)
        if (!bagAdds.isEmpty()) {
            var aReq = new BagAddItemReq(req.getRoleId(), bagType, bagAdds);
            var aResp = bagFeign.add(aReq);
            if (aResp == null || !aResp.ok()) {
                String err = (aResp == null) ? "BAG_ADD_FAIL" : aResp.error();
                return GiftDTOs.OpenResp.builder().ok(false).error(err).consumed(0).build();
            }
        }

        // Build response
        List<GiftDTOs.RollItem> bagItems = bagAdds.stream()
                .map(it -> new GiftDTOs.RollItem(it.itemId(), it.count()))
                .collect(Collectors.toList());

        return GiftDTOs.OpenResp.builder()
                .ok(true)
                .error(null)
                .consumed(req.getCount())
                .bagItems(bagItems)
                .walletAdds(walletChanged)
                .build();
    }
}