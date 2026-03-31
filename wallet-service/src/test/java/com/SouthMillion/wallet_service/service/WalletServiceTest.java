package com.SouthMillion.wallet_service.service;

import com.SouthMillion.wallet_service.entity.WalletAccount;
import com.SouthMillion.wallet_service.entity.WalletLedger;
import com.SouthMillion.wallet_service.repository.WalletAccountRepository;
import com.SouthMillion.wallet_service.repository.WalletLedgerRepository;
import com.SouthMillion.wallet_service.service.client.ItemMetaFeign;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private WalletAccountRepository accRepo;

    @Mock
    private WalletLedgerRepository ledRepo;

    @Mock
    private ItemMetaFeign itemMeta;

    @InjectMocks
    private WalletService walletService;

    private static final String ROLE_ID = "1";
    private static final Long ROLE_LONG_ID = 1L;
    private static final long GOLD_ITEM_ID = 1L;

    // Helper – tao WalletAccount gia
    private WalletAccount account(long id, long balance) {
        WalletAccount a = new WalletAccount();
        a.setId(id);
        a.setRoleId(ROLE_LONG_ID);
        a.setItemId(GOLD_ITEM_ID);
        a.setBalance(balance);
        return a;
    }

    // Helper – BatchReq voi 1 change
    private WalletDTOs.BatchReq batchReq(long amount, String idemKey) {
        var change = WalletDTOs.Change.builder()
                .itemId(GOLD_ITEM_ID)
                .amount(amount)
                .build();
        return WalletDTOs.BatchReq.builder()
                .roleId(ROLE_ID)
                .changes(List.of(change))
                .idemKey(idemKey)
                .reason(1)
                .build();
    }

    // =========================================================
    // batchAdd
    // =========================================================
    @Nested
    @DisplayName("batchAdd()")
    class BatchAdd {

        @Test
        @DisplayName("TC-WAL-001 [P] Nap gold thanh cong – balance tang dung")
        void batchAdd_success() {
            given(ledRepo.findByIdemKey(anyString())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 1)));

            WalletAccount acc = account(10L, 500L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc));
            given(accRepo.applyDeltaIfEnough(eq(10L), eq(100L), anyLong())).willReturn(1);

            WalletAccount updated = account(10L, 600L);
            given(accRepo.findById(10L)).willReturn(Optional.of(updated));
            given(ledRepo.save(any(WalletLedger.class))).willAnswer(inv -> inv.getArgument(0));

            WalletDTOs.MutateResp resp = walletService.batchAdd(batchReq(100L, "idem-add-1"));

            assertThat(resp.ok()).isTrue();
            assertThat(resp.newBalances()).containsEntry(GOLD_ITEM_ID, 600L);
        }

        @Test
        @DisplayName("TC-WAL-003 [I] Idempotency – cung idemKey goi lan 2 tra ve balance hien tai")
        void batchAdd_idempotent_returnsCurrent() {
            WalletLedger existingLed = new WalletLedger();
            given(ledRepo.findByIdemKey("idem-dup")).willReturn(Optional.of(existingLed));

            WalletAccount acc = account(10L, 700L);
            given(accRepo.findByRoleIdAndItemIdIn(eq(ROLE_LONG_ID), anyList()))
                    .willReturn(List.of(acc));

            WalletDTOs.MutateResp resp = walletService.batchAdd(batchReq(100L, "idem-dup"));

            assertThat(resp.ok()).isTrue();
            // Khong goi applyDeltaIfEnough
            then(accRepo).should(never()).applyDeltaIfEnough(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-WAL-004 [N] Item khong phai virtual currency tra ve ok=false")
        void batchAdd_nonVirtualItem_returnsFail() {
            given(ledRepo.findByIdemKey(any())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 0)));

            WalletDTOs.MutateResp resp = walletService.batchAdd(batchReq(100L, null));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.error()).contains("ITEM_NOT_VIRTUAL");
        }

        @Test
        @DisplayName("TC-WAL-005 [N] Amount am tra ve ok=false")
        void batchAdd_negativeAmount_returnsFail() {
            given(ledRepo.findByIdemKey(any())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 1)));

            WalletDTOs.MutateResp resp = walletService.batchAdd(batchReq(-50L, null));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.error()).contains("AMOUNT_MUST_POSITIVE");
        }
    }

    // =========================================================
    // batchCost
    // =========================================================
    @Nested
    @DisplayName("batchCost()")
    class BatchCost {

        @Test
        @DisplayName("TC-WAL-010 [P] Tru tien du so du thanh cong")
        void batchCost_success() {
            given(ledRepo.findByIdemKey(any())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 1)));

            WalletAccount acc = account(10L, 500L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc));
            given(accRepo.applyDeltaIfEnough(eq(10L), eq(-200L), anyLong())).willReturn(1);
            given(ledRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            WalletAccount remaining = account(10L, 300L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc))
                    .willReturn(Optional.of(remaining));

            WalletDTOs.MutateResp resp = walletService.batchCost(batchReq(200L, "idem-cost-1"));

            assertThat(resp.ok()).isTrue();
            assertThat(resp.newBalances()).containsEntry(GOLD_ITEM_ID, 300L);
        }

        @Test
        @DisplayName("TC-WAL-012 [N] So du khong du – nem IllegalStateException")
        void batchCost_insufficientFunds_throws() {
            given(ledRepo.findByIdemKey(any())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 1)));

            WalletAccount acc = account(10L, 100L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc));
            // applyDeltaIfEnough tra ve 0 = khong du tien
            given(accRepo.applyDeltaIfEnough(eq(10L), eq(-500L), anyLong())).willReturn(0);

            assertThatThrownBy(() -> walletService.batchCost(batchReq(500L, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("INSUFFICIENT_FUNDS");
        }

        @Test
        @DisplayName("TC-WAL-013 [I] Idempotency khi tru – chi tru 1 lan")
        void batchCost_idempotent_notDeductedAgain() {
            WalletLedger existing = new WalletLedger();
            given(ledRepo.findByIdemKey("idem-cost-dup")).willReturn(Optional.of(existing));
            given(accRepo.findByRoleIdAndItemIdIn(eq(ROLE_LONG_ID), anyList()))
                    .willReturn(List.of(account(10L, 300L)));

            WalletDTOs.MutateResp resp = walletService.batchCost(batchReq(200L, "idem-cost-dup"));

            assertThat(resp.ok()).isTrue();
            then(accRepo).should(never()).applyDeltaIfEnough(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-WAL-011 [B] Tru toan bo so du – balance = 0")
        void batchCost_allBalance_balanceIsZero() {
            given(ledRepo.findByIdemKey(any())).willReturn(Optional.empty());
            given(itemMeta.batchMeta(anyList()))
                    .willReturn(Map.of(1, Map.of("isVirtual", 1)));

            WalletAccount acc = account(10L, 500L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc));
            given(accRepo.applyDeltaIfEnough(eq(10L), eq(-500L), anyLong())).willReturn(1);
            given(ledRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            WalletAccount zero = account(10L, 0L);
            given(accRepo.findByRoleIdAndItemId(ROLE_LONG_ID, GOLD_ITEM_ID))
                    .willReturn(Optional.of(acc))
                    .willReturn(Optional.of(zero));

            WalletDTOs.MutateResp resp = walletService.batchCost(batchReq(500L, null));

            assertThat(resp.ok()).isTrue();
            assertThat(resp.newBalances()).containsEntry(GOLD_ITEM_ID, 0L);
        }
    }

    // =========================================================
    // get
    // =========================================================
    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("TC-WAL-020 [P] Lay so du dung")
        void get_returnsCorrectBalance() {
            WalletAccount acc = account(10L, 850L);
            given(accRepo.findByRoleIdAndItemIdIn(ROLE_LONG_ID, List.of(GOLD_ITEM_ID)))
                    .willReturn(List.of(acc));

            WalletDTOs.BalancesResp resp = walletService.get(ROLE_LONG_ID, List.of(GOLD_ITEM_ID));

            assertThat(resp.roleId()).isEqualTo(ROLE_ID);
            assertThat(resp.balances()).containsEntry(GOLD_ITEM_ID, 850L);
        }

        @Test
        @DisplayName("TC-WAL-022 [P] Role chua co tai khoan – tra ve balance = 0")
        void get_noAccount_returnsZero() {
            given(accRepo.findByRoleIdAndItemIdIn(ROLE_LONG_ID, List.of(GOLD_ITEM_ID)))
                    .willReturn(List.of());

            WalletDTOs.BalancesResp resp = walletService.get(ROLE_LONG_ID, List.of(GOLD_ITEM_ID));

            assertThat(resp.balances()).containsEntry(GOLD_ITEM_ID, 0L);
        }
    }
}
