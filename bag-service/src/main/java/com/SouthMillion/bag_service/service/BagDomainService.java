package com.SouthMillion.bag_service.service;

import com.SouthMillion.bag_service.enity.BagItem;
import com.SouthMillion.bag_service.mapper.ItemViewMapper;
import com.SouthMillion.bag_service.repository.BagEventDedupRepository;
import com.SouthMillion.bag_service.repository.BagItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BagDomainService {

    private final BagItemRepository repo;
    private final BagEventDedupRepository dedupRepo;

    // ====== Query ======
    @Cacheable(cacheNames = "bag:items", key = "#roleId")
    public List<BagDTOs.ItemView> list(String roleId) {
        return repo.findAllByRoleId(roleId).stream()
                .map(ItemViewMapper::from)
                .collect(Collectors.toList());
    }

    // ====== Grant/Add ======
    @Transactional
    public List<BagDTOs.ItemView> grant(String userId, String roleId,
                                        List<BagDTOs.GrantItem> items, String eventId) {
        if (!dedupRepo.insertIgnore(eventId)) {
            log.info("Grant ignored (idempotent): eventId={}", eventId);
            return List.of();
        }

        List<BagDTOs.ItemView> out = new ArrayList<>(items.size());

        for (var i : items) {
            var bi = BagItem.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .roleId(roleId)
                    .itemId(i.getItemId())
                    .num(i.getNum() == null ? 1L : i.getNum().longValue())
                    .bind(Boolean.TRUE.equals(i.getBind()))
                    .expireAt(i.getExpireAt())   // có thể null
                    // .version(0L)  // ❌ BỎ DÒNG NÀY
                    .build();

            // KHÔNG cần set createdAt/updatedAt thủ công
            repo.save(bi); // giờ sẽ là persist (INSERT), không còn stale

            out.add(BagDTOs.ItemView.builder()
                    .id(bi.getId())
                    .roleId(bi.getRoleId())
                    .itemId(bi.getItemId())
                    .num(bi.getNum())
                    .bind(bi.getBind())
                    .expireAt(bi.getExpireAt())
                    .build());
        }
        return out;
    }


    // ====== Use/Consume ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public void use(String roleId, BagDTOs.UseItemReq req) {
        int changed = repo.consume(roleId, req.getItemId(), req.getNum());
        if (changed <= 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm hoặc không tồn tại.");
        }
        repo.cleanupZero(roleId, req.getItemId());
        // TODO: publish Kafka event "bag.item.used" nếu cần
    }

    // ====== Sell/Discard ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public BagDTOs.SellResult sell(String roleId, BagDTOs.SellItemReq req) {
        int changed = repo.consume(roleId, req.getItemId(), req.getNum());
        if (changed <= 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm để bán.");
        }
        repo.cleanupZero(roleId, req.getItemId());
        long gold = Optional.ofNullable(req.getUnitPrice()).orElse(0L) * req.getNum();
        // TODO: emit event/wallet-service để cộng gold
        return new BagDTOs.SellResult(req.getItemId(), req.getNum(), gold);
    }
}