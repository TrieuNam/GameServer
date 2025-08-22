package com.SouthMillion.bag_service.service;

import com.SouthMillion.bag_service.config.BagConfigCache;
import com.SouthMillion.bag_service.enity.BagMeta;
import com.SouthMillion.bag_service.enity.BagSlot;
import com.SouthMillion.bag_service.repository.BagMetaRepository;
import com.SouthMillion.bag_service.repository.BagSlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BagService {

    private final BagMetaRepository metaRepo;
    private final BagSlotRepository slotRepo;
    private final ItemMetaService itemMeta;
    private final BagConfigCache bagCfg;
    private final EntityManager em;

    private int defaultStartCapacity(byte bagType) {
        var b = bagCfg.getCfg().bag.stream().filter(x -> ((Number)x.get("bag_id")).intValue() == bagType)
                .findFirst().orElse(Map.of("start_num", 80));
        return ((Number)b.getOrDefault("start_num", 80)).intValue();
    }

    private int maxCapacity(byte bagType) {
        var b = bagCfg.getCfg().bag.stream().filter(x -> ((Number)x.get("bag_id")).intValue() == bagType)
                .findFirst().orElse(Map.of("max_num", 300));
        return ((Number)b.getOrDefault("max_num", 300)).intValue();
    }

    private BagMeta ensureMeta(String roleId, byte bagType) {
        return metaRepo.findById(new BagMeta.BagMetaId(roleId, bagType))
                .orElseGet(() -> metaRepo.save(BagMeta.builder()
                        .id(new BagMeta.BagMetaId(roleId, bagType))
                        .capacity(defaultStartCapacity(bagType))
                        .version(0)
                        .build()));
    }

    private static LocalDateTime toLdt(Long epochSec) {
        return epochSec == null ? null : LocalDateTime.ofEpochSecond(epochSec, 0, ZoneOffset.UTC);
    }

    private static Long toEpoch(LocalDateTime ldt) {
        return ldt == null ? null : ldt.toEpochSecond(ZoneOffset.UTC);
    }

    private static BagDTOs.BagSlotView toView(BagSlot s) {
        return new BagDTOs.BagSlotView(
                s.getSlotIndex(),
                s.getItemId(),
                s.getCount(),
                toEpoch(s.getExpireAt()),
                s.isBind()
        );
    }

    // ====================== internal ======================

    @Transactional
    public BagDTOs.AddItemResp addItems(BagDTOs.AddItemReq req) {
        BagMeta meta = ensureMeta(req.roleId(), req.bagType());
        List<BagSlot> slots = slotRepo.findByRoleAndBag(req.roleId(), req.bagType());

        // 1) Validate & reject virtual items
        Set<Integer> reqIds = req.items().stream().map(BagDTOs.ItemDelta::itemId).collect(Collectors.toSet());
        Map<Integer, ItemMetaService.Meta> metas = itemMeta.getMetas(reqIds);
        List<Integer> virtualIds = req.items().stream()
                .map(it -> metas.getOrDefault(it.itemId(), new ItemMetaService.Meta(it.itemId(), 1, false, null)))
                .filter(ItemMetaService.Meta::isVirtual)
                .map(ItemMetaService.Meta::itemId)
                .distinct()
                .toList();
        if (!virtualIds.isEmpty()) {
            throw new IllegalArgumentException("VIRTUAL_ITEM_USE_WALLET:" + virtualIds);
        }

        // 2) Chuẩn hoá id & aggregate số lượng cần thêm
        Map<Integer, Long> toAdd = new HashMap<>();
        // NEW: ghi nhận yêu cầu "singleStack" theo normalizedId
        Map<Integer, Boolean> forceSingleByNormId = new HashMap<>();
        for (var it : req.items()) {
            ItemMetaService.Meta m = metas.getOrDefault(it.itemId(), new ItemMetaService.Meta(it.itemId(), 1, false, null));
            int normId = (m.normalizedId() != null) ? m.normalizedId() : it.itemId();
            toAdd.merge(normId, it.count(), Long::sum);
            if (Boolean.TRUE.equals(it.singleStack())) {
                forceSingleByNormId.put(normId, true);
            }
        }

        // 3) Load pileLimit theo normalized ids
        Map<Integer, ItemMetaService.Meta> normMetas = itemMeta.getMetas(toAdd.keySet());

        // 4) Chuẩn bị stack hiện có (ưu tiên slot hết hạn sớm, số lượng ít trước)
        Map<Integer, List<BagSlot>> stackMap = new HashMap<>();
        for (var s : slots) stackMap.computeIfAbsent(s.getItemId(), k -> new ArrayList<>()).add(s);
        stackMap.values().forEach(list -> list.sort(Comparator
                .comparing(BagSlot::getExpireAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingLong(BagSlot::getCount)));

        int capacity = meta.getCapacity();
        int used = slots.size();

        List<BagSlot> mutated = new ArrayList<>();
        Map<Integer, Long> added    = new HashMap<>(); // itemId -> đã thêm
        Map<Integer, Long> overflow = new HashMap<>(); // itemId -> còn dư (không đủ chỗ)

        // 5) Fill-in
        for (var e : toAdd.entrySet()) {
            int itemId = e.getKey();
            long remain = e.getValue();
            int pile = normMetas.getOrDefault(itemId, new ItemMetaService.Meta(itemId, 1, false, null)).pileLimit();

            // NEW: nếu request yêu cầu singleStack -> bỏ qua pileLimit (cho phép dồn hết vào 1 stack)
            if (Boolean.TRUE.equals(forceSingleByNormId.get(itemId))) {
                pile = Integer.MAX_VALUE; // chỉ hiệu lực trong request này, không hard-code theo item
            }

            // 5.1 Top-up các stack chưa đầy
            List<BagSlot> stacks = stackMap.getOrDefault(itemId, new ArrayList<>());
            for (BagSlot s : stacks) {
                if (remain <= 0) break;
                if (s.getCount() >= pile) continue;
                long can = pile - s.getCount();
                long inc = Math.min(can, remain);
                if (inc <= 0) continue;
                s.setCount(s.getCount() + inc);
                mutated.add(s);
                remain -= inc;
                added.merge(itemId, inc, Long::sum);
            }

            // 5.2 Tạo stack mới nếu còn sức chứa
            while (remain > 0) {
                if (used >= capacity) {
                    overflow.merge(itemId, remain, Long::sum);
                    break;
                }
                long inc = Math.min(remain, pile);
                int nextIndex = used; // sẽ re-index/sort khi view
                BagSlot s = BagSlot.builder()
                        .roleId(req.roleId()).bagType(req.bagType()).slotIndex(nextIndex)
                        .itemId(itemId).count(inc)
                        .bind(false)
                        .expireAt(toLdt(null))
                        .extraJson(null)
                        .build();
                slotRepo.save(s);
                mutated.add(s);
                stackMap.computeIfAbsent(itemId, k -> new ArrayList<>()).add(s);
                used++;
                remain -= inc;
                added.merge(itemId, inc, Long::sum);

                // NEW: nếu singleStack => chỉ cần 1 slot, sau vòng lặp này remain sẽ về 0 ngay lần đầu.
            }
        }

        if (!mutated.isEmpty()) slotRepo.saveAll(mutated);

        Map<Integer, Long> addedOut = added.isEmpty() ? Map.of() : added;
        Map<Integer, Long> overOut  = overflow.isEmpty() ? null  : overflow;

        return new BagDTOs.AddItemResp(addedOut, overOut, null);
    }

    @Transactional
    public BagDTOs.OkResp consume(BagDTOs.ConsumeReq req) {
        // 1) Validate & reject virtual costs
        Set<Integer> ids = req.items().stream().map(BagDTOs.ItemDelta::itemId).collect(Collectors.toSet());
        Map<Integer, ItemMetaService.Meta> metas = itemMeta.getMetas(ids);
        List<Integer> virtualIds = req.items().stream()
                .map(it -> metas.getOrDefault(it.itemId(), new ItemMetaService.Meta(it.itemId(), 1, false, null)))
                .filter(ItemMetaService.Meta::isVirtual)
                .map(ItemMetaService.Meta::itemId)
                .distinct()
                .toList();
        if (!virtualIds.isEmpty()) {
            throw new IllegalArgumentException("VIRTUAL_ITEM_USE_WALLET:" + virtualIds);
        }

        // 2) Chuẩn bị index theo itemId
        List<BagSlot> slots = slotRepo.findByRoleAndBag(req.roleId(), req.bagType());
        Map<Integer, List<BagSlot>> byItem = new HashMap<>();
        for (var s : slots) byItem.computeIfAbsent(s.getItemId(), k -> new ArrayList<>()).add(s);
        byItem.values().forEach(list -> list.sort(Comparator
                .comparing(BagSlot::getExpireAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingLong(BagSlot::getCount)));

        // 3) Check đủ số lượng
        Map<Integer, Long> need = new HashMap<>();
        req.items().forEach(it -> need.merge(it.itemId(), it.count(), Long::sum)); // <<<< dùng count()
        for (var e : need.entrySet()) {
            long have = byItem.getOrDefault(e.getKey(), List.of()).stream().mapToLong(BagSlot::getCount).sum();
            if (have < e.getValue()) {
                throw new IllegalStateException("NOT_ENOUGH_ITEM:" + e.getKey());
            }
        }

        // 4) Trừ
        List<BagSlot> changed = new ArrayList<>();
        List<BagSlot> toDelete = new ArrayList<>();
        for (var e : need.entrySet()) {
            long remain = e.getValue();
            for (BagSlot s : byItem.get(e.getKey())) {
                if (remain<=0) break;
                long take = Math.min(s.getCount(), remain);
                s.setCount(s.getCount() - take);
                changed.add(s);
                remain -= take;
                if (s.getCount()==0) toDelete.add(s);
            }
        }
        if (!toDelete.isEmpty()) slotRepo.deleteAll(toDelete);
        if (!changed.isEmpty()) slotRepo.saveAll(changed);
        return BagDTOs.OkResp.OK();
    }

    // ====================== public ======================

    @Transactional
    public BagDTOs.BagView sortCompact(BagDTOs.SortReq req) {
        List<BagSlot> slots = slotRepo.findByRoleAndBag(req.roleId(), req.bagType());
        if (slots.isEmpty()) {
            BagMeta meta = ensureMeta(req.roleId(), req.bagType());
            return new BagDTOs.BagView(req.roleId(), req.bagType(), meta.getCapacity(), 0, List.of());
        }

        // Group theo itemId -> merge theo pileLimit
        Map<Integer, List<BagSlot>> group = slots.stream().collect(Collectors.groupingBy(BagSlot::getItemId));
        List<BagSlot> merged = new ArrayList<>();
        for (var e : group.entrySet()) {
            int itemId = e.getKey();
            List<BagSlot> lst = e.getValue();
            int pile = itemMeta.getMetas(List.of(itemId)).get(itemId).pileLimit();

            // Ưu tiên hết hạn sớm
            lst.sort(Comparator.comparing(BagSlot::getExpireAt, Comparator.nullsFirst(Comparator.naturalOrder())));
            long total = lst.stream().mapToLong(BagSlot::getCount).sum();
            List<LocalDateTime> expires = lst.stream().map(BagSlot::getExpireAt).filter(Objects::nonNull).sorted().toList();

            int expIdx = 0;
            while (total > 0) {
                long take = Math.min(total, pile);
                LocalDateTime exp = expIdx < expires.size()? expires.get(expIdx++) : null;
                merged.add(BagSlot.builder()
                        .roleId(req.roleId()).bagType(req.bagType())
                        .slotIndex(0) // sẽ re-index
                        .itemId(itemId).count(take)
                        .bind(false).expireAt(exp).extraJson(null)
                        .build());
                total -= take;
            }
        }

        // Sort merged và re-index
        merged.sort(Comparator
                .comparing(BagSlot::getItemId)
                .thenComparing(BagSlot::getExpireAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        slotRepo.deleteAll(slots);
        for (int i=0;i<merged.size();i++) merged.get(i).setSlotIndex(i);
        slotRepo.saveAll(merged);

        BagMeta meta = ensureMeta(req.roleId(), req.bagType());
        List<BagDTOs.BagSlotView> views = merged.stream().map(BagService::toView).toList();
        return new BagDTOs.BagView(req.roleId(), req.bagType(), meta.getCapacity(), merged.size(), views);
    }

    @Transactional
    public BagDTOs.BagView get(String roleId, byte bagType) {
        BagMeta meta = ensureMeta(roleId, bagType);
        List<BagSlot> slots = slotRepo.findByRoleAndBag(roleId, bagType);
        List<BagDTOs.BagSlotView> views = slots.stream()
                .sorted(Comparator.comparingInt(BagSlot::getSlotIndex))
                .map(BagService::toView)
                .toList();
        return new BagDTOs.BagView(roleId, bagType, meta.getCapacity(), views.size(), views);
    }

    @Transactional
    public BagDTOs.OkResp expand(BagDTOs.ExpandReq req) {
        BagMeta meta = ensureMeta(req.roleId(), req.bagType());
        int newCap = Math.min(meta.getCapacity() + req.slots(), maxCapacity(req.bagType()));
        meta.setCapacity(newCap);
        metaRepo.save(meta);
        // TODO: charge cost if needed based on config
        return BagDTOs.OkResp.OK();
    }
}