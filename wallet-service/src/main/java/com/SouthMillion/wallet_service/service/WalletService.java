package com.SouthMillion.wallet_service.service;

import com.SouthMillion.wallet_service.entity.WalletAccount;
import com.SouthMillion.wallet_service.entity.WalletLedger;
import com.SouthMillion.wallet_service.repository.WalletAccountRepository;
import com.SouthMillion.wallet_service.repository.WalletLedgerRepository;
import com.SouthMillion.wallet_service.service.client.ItemMetaFeign;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletAccountRepository accRepo;
    private final WalletLedgerRepository ledRepo;
    private final ItemMetaFeign itemMeta;

    @Transactional
    public WalletDTOs.MutateResp batchAdd(WalletDTOs.BatchReq req) {
        long now = Instant.now().getEpochSecond();
        Long roleIdLong = Long.parseLong(req.roleId());

        // idempotency (nếu đã xử lý cùng idemKey thì trả về balances hiện tại)
        if (req.idemKey()!=null && !req.idemKey().isBlank()) {
            if (ledRepo.findByIdemKey(req.idemKey()).isPresent()) {
                Map<Long, Long> balances = getBalancesMap(roleIdLong,
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
            WalletAccount acc = accRepo.findByRoleIdAndItemId(roleIdLong, c.itemId())
                    .orElseGet(() -> {
                        WalletAccount a = new WalletAccount();
                        a.setRoleId(roleIdLong);
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
            led.setRoleId(roleIdLong);
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
        Long roleIdLong = Long.parseLong(req.roleId());

        if (req.idemKey()!=null && !req.idemKey().isBlank()) {
            if (ledRepo.findByIdemKey(req.idemKey()).isPresent()) {
                Map<Long, Long> balances = getBalancesMap(roleIdLong,
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

        Map<Long, Long> newBalances = new HashMap<>();
        List<WalletAccount> touched = new ArrayList<>();

        for (var c : req.changes()) {
            WalletAccount acc = accRepo.findByRoleIdAndItemId(roleIdLong, c.itemId())
                    .orElseGet(() -> {
                        WalletAccount a = new WalletAccount();
                        a.setRoleId(roleIdLong);
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

        for (var c : req.changes()) {
            WalletLedger led = new WalletLedger();
            led.setRoleId(roleIdLong);
            led.setItemId(c.itemId());
            led.setDelta(-c.amount());
            led.setReason(req.reason());
            led.setReasonType(req.reasonType());
            led.setIdemKey(req.idemKey());
            led.setCreatedAtEpochSec(now);
            ledRepo.save(led);

            WalletAccount cur = accRepo.findByRoleIdAndItemId(roleIdLong, c.itemId()).orElseThrow();
            newBalances.put(c.itemId(), cur.getBalance());
        }

        return new WalletDTOs.MutateResp(true, null, newBalances, now);
    }

    public WalletDTOs.BalancesResp get(Long roleId, List<Long> itemIds) {
        long now = Instant.now().getEpochSecond();
        Map<Long, Long> map = getBalancesMap(roleId, itemIds);
        return new WalletDTOs.BalancesResp(String.valueOf(roleId), map, now);
    }

    private Map<Long, Long> getBalancesMap(Long roleId, List<Long> itemIds) {
        if (itemIds==null || itemIds.isEmpty()) return Map.of();
        var rows = accRepo.findByRoleIdAndItemIdIn(roleId, itemIds);
        Map<Long, Long> ret = new HashMap<>();
        for (Long id : itemIds) ret.put(id, 0L);
        for (var r : rows) ret.put(r.getItemId(), r.getBalance());
        return ret;
    }

    @Transactional
    public WalletDTOs.BalancesResp info(Long roleId) {
        long now = Instant.now().getEpochSecond();
        var accounts = accRepo.findByRoleId(roleId);
        if (accounts == null || accounts.isEmpty()) {
            return WalletDTOs.BalancesResp.builder()
                    .roleId(String.valueOf(roleId))
                    .balances(Collections.emptyMap())
                    .atEpochSec(now)
                    .build();
        }
        List<Long> ids = accounts.stream().map(WalletAccount::getItemId).distinct().collect(Collectors.toList());
        Map<Long, Boolean> virtualMap = ensureVirtual(ids);
        Map<Long, Long> balances = new LinkedHashMap<>();
        accounts.stream().sorted(Comparator.comparingLong(WalletAccount::getItemId)).forEach(acc -> {
            if (virtualMap.getOrDefault(acc.getItemId(), false)) {
                balances.put(acc.getItemId(), acc.getBalance() == null ? 0L : acc.getBalance());
            }
        });
        return WalletDTOs.BalancesResp.builder()
                .roleId(String.valueOf(roleId))
                .balances(balances)
                .atEpochSec(now)
                .build();
    }

    // ===== đã có sẵn trong code bạn đưa (nhắc lại ở đây để liền mạch) =====
    private Map<Long, Boolean> ensureVirtual(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Integer> itemIds = ids.stream()
                .filter(Objects::nonNull)
                .map(Math::toIntExact)
                .distinct()
                .toList();
        log.debug("[wallet.virtual] checking itemIds={}", itemIds);

        Map<Integer, Map<String, Object>> metas = itemMeta.batchMeta(itemIds);
        Map<Long, Boolean> out = new HashMap<>();
        for (Long id : ids) {
            Map<String, Object> v = metas == null ? Map.of() : metas.getOrDefault(Math.toIntExact(id), Map.of());
            boolean virt = asBooleanFlag(v.get("isVirtual"));
            out.put(id, virt);
        }
        log.debug("[wallet.virtual] result={}", out);
        return out;
    }

    private boolean asBooleanFlag(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return false;
            }
            if ("true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("false".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            try {
                return Integer.parseInt(normalized) != 0;
            } catch (NumberFormatException ignored) {
                log.debug("[wallet.virtual] unsupported string value={}", normalized);
                return false;
            }
        }
        log.debug("[wallet.virtual] unsupported flag type={} value={}", value.getClass().getName(), value);
        return false;
    }
}