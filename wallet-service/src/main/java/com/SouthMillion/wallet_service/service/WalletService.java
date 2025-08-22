package com.SouthMillion.wallet_service.service;

import com.SouthMillion.wallet_service.entity.WalletAccount;
import com.SouthMillion.wallet_service.entity.WalletLedger;
import com.SouthMillion.wallet_service.repository.WalletAccountRepository;
import com.SouthMillion.wallet_service.repository.WalletLedgerRepository;
import com.SouthMillion.wallet_service.service.client.ItemMetaFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletAccountRepository accRepo;
    private final WalletLedgerRepository ledRepo;
    private final ItemMetaFeign itemMeta;

    private Map<Long, Boolean> ensureVirtual(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        String csv = ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
        Map<String, Map<String,Object>> metas = itemMeta.batchMeta(csv);
        Map<Long, Boolean> out = new HashMap<>();
        for (Long id : ids) {
            Map<String,Object> v = metas.getOrDefault(String.valueOf(id), Map.of());
            boolean virt = ((Number)v.getOrDefault("isVirtual", 0)).intValue() == 1;
            out.put(id, virt);
        }
        return out;
    }

    @Transactional
    public WalletDTOs.MutateResp batchAdd(WalletDTOs.BatchReq req) {
        long now = Instant.now().getEpochSecond();

        // idempotency (nếu đã xử lý cùng idemKey thì trả về balances hiện tại)
        if (req.idemKey()!=null && !req.idemKey().isBlank()) {
            if (ledRepo.findByIdemKey(req.idemKey()).isPresent()) {
                Map<Long, Long> balances = getBalancesMap(req.roleId(),
                        req.changes().stream().map(WalletDTOs.Change::itemId).toList());
                return new WalletDTOs.MutateResp(true, null, balances, now);
            }
        }

        // Validate virtual
        Map<Long, Boolean> virt = ensureVirtual(req.changes().stream().map(WalletDTOs.Change::itemId).toList());
        for (var c : req.changes()) {
            if (!virt.getOrDefault(c.itemId(), false)) {
                return new WalletDTOs.MutateResp(false, "ITEM_NOT_VIRTUAL:"+c.itemId(), null, now);
            }
            if (c.amount() <= 0) {
                return new WalletDTOs.MutateResp(false, "AMOUNT_MUST_POSITIVE", null, now);
            }
        }

        Map<Long, Long> newBalances = new HashMap<>();
        for (var c : req.changes()) {
            WalletAccount acc = accRepo.findByRoleIdAndItemId(req.roleId(), c.itemId())
                    .orElseGet(() -> {
                        WalletAccount a = new WalletAccount();
                        a.setRoleId(req.roleId());
                        a.setItemId(c.itemId());
                        a.setBalance(0L);
                        a.setUpdatedAtEpochSec(now);
                        return accRepo.save(a);
                    });

            int n = accRepo.applyDeltaIfEnough(acc.getId(), c.amount(), now);
            if (n == 0) { // không thể xảy ra thiếu tiền khi cộng, trừ khi row biến mất; retry đơn giản
                acc = accRepo.findById(acc.getId()).orElseThrow();
                acc.setBalance(acc.getBalance() + c.amount());
                acc.setUpdatedAtEpochSec(now);
                accRepo.save(acc);
            }
            long cur = accRepo.findById(acc.getId()).orElseThrow().getBalance();
            newBalances.put(c.itemId(), cur);

            WalletLedger led = new WalletLedger();
            led.setRoleId(req.roleId());
            led.setItemId(c.itemId());
            led.setDelta(+c.amount());
            led.setReason(req.reason());
            led.setReasonType(req.reasonType());
            led.setIdemKey(req.idemKey());
            led.setCreatedAtEpochSec(now);
            ledRepo.save(led);
        }

        return new WalletDTOs.MutateResp(true, null, newBalances, now);
    }

    @Transactional
    public WalletDTOs.MutateResp batchCost(WalletDTOs.BatchReq req) {
        long now = Instant.now().getEpochSecond();

        if (req.idemKey()!=null && !req.idemKey().isBlank()) {
            if (ledRepo.findByIdemKey(req.idemKey()).isPresent()) {
                Map<Long, Long> balances = getBalancesMap(req.roleId(),
                        req.changes().stream().map(WalletDTOs.Change::itemId).toList());
                return new WalletDTOs.MutateResp(true, null, balances, now);
            }
        }

        Map<Long, Boolean> virt = ensureVirtual(req.changes().stream().map(WalletDTOs.Change::itemId).toList());
        for (var c : req.changes()) {
            if (!virt.getOrDefault(c.itemId(), false)) {
                return new WalletDTOs.MutateResp(false, "ITEM_NOT_VIRTUAL:"+c.itemId(), null, now);
            }
            if (c.amount() <= 0) {
                return new WalletDTOs.MutateResp(false, "AMOUNT_MUST_POSITIVE", null, now);
            }
        }

        // trừ tiền atomically trong 1 TX: nếu thiếu ở bất kỳ dòng nào => rollback
        Map<Long, Long> newBalances = new HashMap<>();
        List<WalletAccount> touched = new ArrayList<>();

        for (var c : req.changes()) {
            WalletAccount acc = accRepo.findByRoleIdAndItemId(req.roleId(), c.itemId())
                    .orElseGet(() -> {
                        WalletAccount a = new WalletAccount();
                        a.setRoleId(req.roleId());
                        a.setItemId(c.itemId());
                        a.setBalance(0L);
                        a.setUpdatedAtEpochSec(now);
                        return accRepo.save(a);
                    });

            int ok = accRepo.applyDeltaIfEnough(acc.getId(), -c.amount(), now);
            if (ok == 0) {
                throw new IllegalStateException("INSUFFICIENT_FUNDS:" + c.itemId());
            }
            touched.add(acc);
        }

        // ghi ledger & đọc số dư
        for (var c : req.changes()) {
            WalletLedger led = new WalletLedger();
            led.setRoleId(req.roleId());
            led.setItemId(c.itemId());
            led.setDelta(-c.amount());
            led.setReason(req.reason());
            led.setReasonType(req.reasonType());
            led.setIdemKey(req.idemKey());
            led.setCreatedAtEpochSec(now);
            ledRepo.save(led);

            WalletAccount cur = accRepo.findByRoleIdAndItemId(req.roleId(), c.itemId()).orElseThrow();
            newBalances.put(c.itemId(), cur.getBalance());
        }

        return new WalletDTOs.MutateResp(true, null, newBalances, now);
    }

    public WalletDTOs.BalancesResp get(String roleId, List<Long> itemIds) {
        long now = Instant.now().getEpochSecond();
        Map<Long, Long> map = getBalancesMap(roleId, itemIds);
        return new WalletDTOs.BalancesResp(roleId, map, now);
    }

    private Map<Long, Long> getBalancesMap(String roleId, List<Long> itemIds) {
        if (itemIds==null || itemIds.isEmpty()) return Map.of();
        var rows = accRepo.findByRoleIdAndItemIdIn(roleId, itemIds);
        Map<Long, Long> ret = new HashMap<>();
        for (Long id : itemIds) ret.put(id, 0L);
        for (var r : rows) ret.put(r.getItemId(), r.getBalance());
        return ret;
    }
}