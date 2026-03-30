package com.SouthMillion.drop_service.service;


import com.SouthMillion.drop_service.config.AppProperties;
import com.SouthMillion.drop_service.repository.DropRepository;
import com.SouthMillion.drop_service.service.client.BagFeign;
import com.SouthMillion.drop_service.service.client.ItemMetaFeign;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagAddItemResp;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.SouthMillion.dto.drop.RollRequest;
import org.SouthMillion.dto.drop.RollResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DropRoller {
    private final DropRepository repo;
    private final PityService pity;
    private final AppProperties props;
    private final Optional<ItemMetaFeign> itemMeta; // optional
    private final Optional<BagFeign> bagFeign;      // optional

    public RollResult roll(RollRequest req) {
        var compiled = repo.getCompiled(req.getDropId());
        Random rnd = (req.getOptions()!=null && req.getOptions().getForceSeed()!=null)
                ? new Random(req.getOptions().getForceSeed())
                : new Random();

        List<RollResult.Item> items = new ArrayList<>();
        Set<Integer> seenItemId = new HashSet<>();

        boolean pityApplied = false;
        Integer counterBefore = null;
        Integer threshold = null;

        for (int i=0;i<req.getTimes();i++) {
            CompiledDrop.Row row;

            if (i==0 && pity.enabled()) {
                String group = pity.groupOf(req.getDropId(), req.getOptions()==null?null:req.getOptions().getPityGroup());
                counterBefore = pity.get(group, req.getRoleId());
                threshold = pity.thresholdFor(req.getDropId());

                if (counterBefore >= threshold) {
                    List<Integer> rareList = "LIST".equalsIgnoreCase(pity.rareSelectorFor(req.getDropId()))
                            ? pity.rareListFor(req.getDropId()) : List.of();
                    row = compiled.pickRareOnly(rnd, rareList);
                    pity.reset(group, req.getRoleId());
                    pityApplied = true;
                } else {
                    row = compiled.pick(rnd);
                    if (row.broadcast()==1) pity.reset(group, req.getRoleId());
                    else pity.incr(group, req.getRoleId());
                }
            } else {
                row = compiled.pick(rnd);
            }

            if (req.getOptions()!=null && req.getOptions().isNoRepeat()) {
                int tries = 0;
                while (seenItemId.contains(row.itemId()) && tries<10) { row = compiled.pick(rnd); tries++; }
                seenItemId.add(row.itemId());
            }

            items.add(new RollResult.Item(row.itemId(), row.num(), row.bind(), row.broadcast()));
        }

        // optional: validate item ids
        if (props.getItem().isValidate() && itemMeta.isPresent() && !items.isEmpty()) {
            String idsCsv = items.stream().map(i -> Integer.toString(i.getItemId())).distinct().reduce((a,b)->a+","+b).orElse("");
            if (!idsCsv.isBlank()) {
                try {
                    var meta = itemMeta.get().batchMeta(idsCsv);
                    // loại bỏ item notFound (nếu muốn)
                    items.removeIf(it -> {
                        var m = meta.get(String.valueOf(it.getItemId()));
                        return m!=null && Boolean.TRUE.equals(m.get("notFound"));
                    });
                } catch (Exception ignore) {}
            }
        }

        RollResult.ApplyBag apply = null;
        boolean askApply = req.getOptions()!=null && req.getOptions().isApplyToBag();
        if (askApply && props.getBag().isApplyEnabled() && bagFeign.isPresent()) {
            apply = applyToBag(req.getRoleId(), req.getOptions().getBagType(), items);
        } else {
            apply = new RollResult.ApplyBag(askApply, false, Map.of("reason", "apply disabled or no feign"));
        }

        return RollResult.builder()
                .dropId(req.getDropId())
                .rolled(items)
                .pity(new RollResult.PityInfo(Boolean.TRUE.equals(pityApplied), counterBefore, threshold))
                .apply(apply)
                .build();
    }

    private RollResult.ApplyBag applyToBag(String roleId, Integer bagTypeOpt, List<RollResult.Item> items) {
        try {
            int bagType = (bagTypeOpt != null) ? bagTypeOpt : props.getBag().getDefaultBagType();
            Map<String, Map<String, Object>> metas = loadItemMetas(items);

            // Map qua DTO request (record)
            List<BagAddItemReq.Item> reqItems =
                    (items == null ? List.<RollResult.Item>of() : items).stream()
                            .map(it -> {
                                Map<String, Object> meta = metas.get(String.valueOf(it.getItemId()));
                                int itemBagType = bagTypeOpt != null ? bagType : resolveBagType(meta, bagType);
                                return BagAddItemReq.Item.builder()
                                        .itemId(it.getItemId())
                                        .amount(it.getNum())
                                        .quality(resolveQuality(meta))
                                        .bagType(itemBagType)
                                        .bound(it.getBind() == 1)
                                        .build();
                            })
                            .toList();

            BagAddItemReq req = BagAddItemReq.builder()
                    .userId(1L) // audit field - roleId used for bag operations
                    .roleId(Long.parseLong(roleId))
                    .items(reqItems)
                    .source("DROP_ROLL")
                    .build();

            BagAddItemResp resp = bagFeign.get().add(req);

            boolean ok = resp != null && resp.isSuccess();

            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("ok", ok);
            raw.put("error", resp != null ? resp.error() : "null response from bag-service");
            // (tuỳ chọn) echo lại request để dễ debug
            raw.put("request", Map.of("roleId", roleId, "bagType", bagType, "items", reqItems));

            return new RollResult.ApplyBag(true, ok, raw);

        } catch (Exception e) {
            return new RollResult.ApplyBag(true, false, Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Map<String, Object>> loadItemMetas(List<RollResult.Item> items) {
        if (items == null || items.isEmpty() || itemMeta.isEmpty()) return Map.of();

        String idsCsv = items.stream()
                .map(it -> Integer.toString(it.getItemId()))
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        if (idsCsv.isBlank()) return Map.of();

        try {
            Map<String, Map<String, Object>> metas = itemMeta.get().batchMeta(idsCsv);
            return metas == null ? Map.of() : metas;
        } catch (Exception ignore) {
            return Map.of();
        }
    }

    private int resolveQuality(Map<String, Object> meta) {
        return Math.max(1, readInt(meta, 1, "quality", "color", "q"));
    }

    private int resolveBagType(Map<String, Object> meta, int defaultValue) {
        return Math.max(0, readInt(meta, defaultValue, "bagType", "bag_type"));
    }

    private int readInt(Map<String, Object> meta, int defaultValue, String... keys) {
        if (meta == null) return defaultValue;
        for (String key : keys) {
            Object value = meta.get(key);
            if (value instanceof Number num) return num.intValue();
            if (value != null) {
                try {
                    return Integer.parseInt(String.valueOf(value));
                } catch (Exception ignore) {}
            }
        }
        return defaultValue;
    }

}
