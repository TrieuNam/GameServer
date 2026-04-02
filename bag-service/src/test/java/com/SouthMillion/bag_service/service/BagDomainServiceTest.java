package com.SouthMillion.bag_service.service;

import com.SouthMillion.bag_service.enity.BagItem;
import com.SouthMillion.bag_service.repository.BagEventDedupRepository;
import com.SouthMillion.bag_service.repository.BagItemRepository;
import com.SouthMillion.bag_service.repository.RecycleProgressRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.SouthMillion.dto.bag.BagDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BagDomainService Tests")
class BagDomainServiceTest {

    @Mock
    private BagItemRepository repo;

    @Mock
    private BagEventDedupRepository dedupRepo;

    @Mock
    private RecycleProgressRepository recycleRepo;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BagDomainService bagDomainService;

    private static final Long ROLE_ID = 1L;
    private static final String USER_ID = "user-001";

    // ── helpers ──────────────────────────────────────────────
    private BagItem item(String id, int itemId, long num) {
        BagItem bi = new BagItem();
        bi.setId(id);
        bi.setRoleId(ROLE_ID);
        bi.setUserId(USER_ID);
        bi.setItemId(itemId);
        bi.setNum(num);
        bi.setBind(false);
        bi.setQuality(1);
        bi.setBagType(0);
        return bi;
    }

    private BagDTOs.GrantItem grantItem(int itemId, int num) {
        return BagDTOs.GrantItem.builder()
                .itemId(itemId)
                .num(num)
                .bind(0)
                .build();
    }

    // =========================================================
    // list
    // =========================================================
    @Nested
    @DisplayName("list()")
    class ListItems {

        @Test
        @DisplayName("TC-BAG-001 [P] Lay tui do thanh cong")
        void list_returnsItems() {
            given(repo.findAllByRoleId(ROLE_ID))
                    .willReturn(List.of(item("id1", 1001, 5L), item("id2", 1002, 3L)));

            List<BagDTOs.ItemView> result = bagDomainService.list(ROLE_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(BagDTOs.ItemView::getItemId)
                    .containsExactlyInAnyOrder(1001, 1002);
        }

        @Test
        @DisplayName("TC-BAG-002 [P] Tui do trong – tra ve list rong")
        void list_emptyBag() {
            given(repo.findAllByRoleId(ROLE_ID)).willReturn(List.of());

            List<BagDTOs.ItemView> result = bagDomainService.list(ROLE_ID);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // grant
    // =========================================================
    @Nested
    @DisplayName("grant()")
    class Grant {

        @Test
        @DisplayName("TC-BAG-010 [P] Cap item moi thanh cong")
        void grant_success() {
            given(dedupRepo.insertIgnore("evt-001")).willReturn(true);
            given(repo.findExact(ROLE_ID, 1001, false, 1, 0, null)).willReturn(java.util.Optional.empty());
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            List<BagDTOs.ItemView> result = bagDomainService.grant(
                    USER_ID, ROLE_ID,
                    List.of(grantItem(1001, 5)),
                    "evt-001"
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemId()).isEqualTo(1001);
            assertThat(result.get(0).getNum()).isEqualTo(5);
            assertThat(result.get(0).getQuality()).isEqualTo(1);
            assertThat(result.get(0).getBagType()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-BAG-012 [I] Idempotency – cung eventId goi lan 2 tra ve list rong")
        void grant_duplicateEventId_returnsEmpty() {
            given(dedupRepo.insertIgnore("evt-dup")).willReturn(false);

            List<BagDTOs.ItemView> result = bagDomainService.grant(
                    USER_ID, ROLE_ID,
                    List.of(grantItem(1001, 5)),
                    "evt-dup"
            );

            assertThat(result).isEmpty();
            then(repo).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-BAG-013 [P] Cap nhieu item cung luc")
        void grant_multipleItems() {
            given(dedupRepo.insertIgnore("evt-multi")).willReturn(true);
            given(repo.findExact(ROLE_ID, 1001, false, 1, 0, null)).willReturn(java.util.Optional.empty());
            given(repo.findExact(ROLE_ID, 1002, false, 1, 0, null)).willReturn(java.util.Optional.empty());
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            List<BagDTOs.ItemView> result = bagDomainService.grant(
                    USER_ID, ROLE_ID,
                    List.of(grantItem(1001, 3), grantItem(1002, 7)),
                    "evt-multi"
            );

            assertThat(result).hasSize(2);
            then(repo).should(times(2)).save(any(BagItem.class));
        }

        @Test
        @DisplayName("TC-BAG-014 [P] Cap item co bind=true – bind duoc luu dung")
        void grant_bindItem() {
            given(dedupRepo.insertIgnore("evt-bind")).willReturn(true);
            given(repo.findExact(ROLE_ID, 2001, true, 1, 0, null)).willReturn(java.util.Optional.empty());
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            BagDTOs.GrantItem bindItem = BagDTOs.GrantItem.builder()
                    .itemId(2001)
                    .num(1)
                    .bind(1)
                    .build();

            bagDomainService.grant(USER_ID, ROLE_ID, List.of(bindItem), "evt-bind");

            // Verify save duoc goi voi item co bind=true
            then(repo).should().save(argThat(bi -> Boolean.TRUE.equals(bi.getBind())));
        }

        @Test
        @DisplayName("TC-BAG-015 [P] Grant cung item/bind/expire thi stack vao row co san")
        void grant_sameItem_stacksExistingRow() {
            BagItem existing = item("id-stack", 1001, 5L);
            given(dedupRepo.insertIgnore("evt-stack")).willReturn(true);
            given(repo.findExact(ROLE_ID, 1001, false, 1, 0, null)).willReturn(java.util.Optional.of(existing));
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            List<BagDTOs.ItemView> result = bagDomainService.grant(
                    USER_ID, ROLE_ID,
                    List.of(grantItem(1001, 3)),
                    "evt-stack"
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNum()).isEqualTo(8);
            then(repo).should(never()).save(argThat(bi -> "id-stack".equals(bi.getId()) && bi.getNum() == 5L));
            then(repo).should().save(argThat(bi -> "id-stack".equals(bi.getId()) && bi.getNum() == 8L));
        }
    }

    // =========================================================
    // use
    // =========================================================
    @Nested
    @DisplayName("use()")
    class UseItem {

        @Test
        @DisplayName("TC-BAG-020 [P] Dung item thanh cong – num giam dung")
        void use_success() {
            BagItem stack = item("id1", 1001, 5L);
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.findAllByRoleIdAndItemId(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            BagDTOs.UseItemReq req = new BagDTOs.UseItemReq();
            req.setItemId(1001); req.setNum(3);

            assertThatCode(() -> bagDomainService.use(ROLE_ID, req))
                    .doesNotThrowAnyException();

            then(repo).should().save(argThat(bi -> "id1".equals(bi.getId()) && bi.getNum() == 2L));
            then(repo).should(never()).consume(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("TC-BAG-021 [N] Khong du so luong – nem IllegalStateException")
        void use_insufficientQuantity_throws() {
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(item("id1", 1001, 5L)));

            BagDTOs.UseItemReq req = new BagDTOs.UseItemReq();
            req.setItemId(1001); req.setNum(10);

            assertThatThrownBy(() -> bagDomainService.use(ROLE_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Không đủ số lượng");
        }

        @Test
        @DisplayName("TC-BAG-024 [B] Dung het toan bo so luong – stack bang 0 bi xoa")
        void use_consumeAll_cleanupCalled() {
            BagItem stack = item("id1", 1001, 5L);
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.findAllByRoleIdAndItemId(ROLE_ID, 1001)).willReturn(List.of());

            BagDTOs.UseItemReq req = new BagDTOs.UseItemReq();
            req.setItemId(1001); req.setNum(5);

            bagDomainService.use(ROLE_ID, req);

            then(repo).should().delete(stack);
        }

        @Test
        @DisplayName("TC-BAG-025 [R] Box split 2 stack, dung 1 chi tru dung 1")
        void use_splitStacks_consumesExactRequestedOnly() {
            BagItem stack1 = item("id1", 40004, 1L);
            BagItem stack2 = item("id2", 40004, 1L);
            List<BagItem> stacks = new ArrayList<>(List.of(stack1, stack2));
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 40004)).willReturn(stacks);
            given(repo.findAllByRoleIdAndItemId(ROLE_ID, 40004)).willAnswer(inv -> new ArrayList<>(stacks));
            willAnswer(inv -> {
                stacks.remove(inv.getArgument(0));
                return null;
            }).given(repo).delete(any(BagItem.class));

            BagDTOs.UseItemReq req = new BagDTOs.UseItemReq();
            req.setItemId(40004); req.setNum(1);

            assertThatCode(() -> bagDomainService.use(ROLE_ID, req)).doesNotThrowAnyException();

            long remaining = stacks.stream().mapToLong(bi -> bi.getNum() == null ? 0L : bi.getNum()).sum();
            assertThat(remaining).isEqualTo(1L);
        }
    }

    // =========================================================
    // sell
    // =========================================================
    @Nested
    @DisplayName("sell()")
    class SellItem {

        @Test
        @DisplayName("TC-BAG-030 [P] Ban item thanh cong – gold duoc tinh dung")
        void sell_success() {
            BagItem stack = item("id1", 1001, 9L);
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.findAllByRoleIdAndItemId(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.save(any(BagItem.class))).willAnswer(inv -> inv.getArgument(0));

            BagDTOs.SellItemReq req = new BagDTOs.SellItemReq();
            req.setItemId(1001); req.setNum(5); req.setUnitPrice(10L);

            BagDTOs.SellResult result = bagDomainService.sell(ROLE_ID, req);

            assertThat(result.getItemsSold()).isEqualTo(5);
            assertThat(result.getGoldEarned()).isEqualTo(50L); // 5 * 10
            then(repo).should().save(argThat(bi -> "id1".equals(bi.getId()) && bi.getNum() == 4L));
        }

        @Test
        @DisplayName("TC-BAG-032 [N] Khong du so luong de ban – nem IllegalStateException")
        void sell_insufficientQuantity_throws() {
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(item("id1", 1001, 5L)));

            BagDTOs.SellItemReq req = new BagDTOs.SellItemReq();
            req.setItemId(1001); req.setNum(10); req.setUnitPrice(5L);

            assertThatThrownBy(() -> bagDomainService.sell(ROLE_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Không đủ số lượng");
        }

        @Test
        @DisplayName("TC-BAG-033 [B] Ban item unitPrice=null – gold = 0")
        void sell_nullUnitPrice_goldIsZero() {
            BagItem stack = item("id1", 1001, 3L);
            given(repo.findAllByRoleIdAndItemIdForUpdate(ROLE_ID, 1001)).willReturn(List.of(stack));
            given(repo.findAllByRoleIdAndItemId(ROLE_ID, 1001)).willReturn(List.of());

            BagDTOs.SellItemReq req = new BagDTOs.SellItemReq();
            req.setItemId(1001); req.setNum(3); req.setUnitPrice(null);

            BagDTOs.SellResult result = bagDomainService.sell(ROLE_ID, req);

            assertThat(result.getGoldEarned()).isEqualTo(0L);
            then(repo).should().delete(stack);
        }
    }
}
