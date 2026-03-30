package com.SouthMillion.gift_service.service;

import com.SouthMillion.gift_service.config.GiftConfigCache;
import com.SouthMillion.gift_service.service.client.BagInternalFeign;
import com.SouthMillion.gift_service.service.client.ItemMetaFeign;
import com.SouthMillion.gift_service.service.client.WalletFeignClient;
import org.SouthMillion.dto.bag.BagAddItemResp;
import org.SouthMillion.dto.bag.BagOkResp;
import org.SouthMillion.dto.gift.GiftDTOs;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
@DisplayName("GiftService Tests")
class GiftServiceTest {

    @Mock private GiftConfigCache    cfg;
    @Mock private ItemMetaFeign      itemMetaFeign;
    @Mock private BagInternalFeign   bagFeign;
    @Mock private WalletFeignClient  walletFeign;

    @Mock private BagOkResp          bagOkResp;
    @Mock private BagAddItemResp     bagAddItemResp;
    @Mock private ResultDTO          walletResult;
    @Mock private WalletDTOs.MutateResp mutateResp;

    @InjectMocks private GiftService giftService;

    /** Create a simple DefGift (type=1) box with one item */
    private GiftConfigCache.GiftBox defBox(long giftId, long itemId, long count) {
        var item = new GiftConfigCache.GiftItem(itemId, count, 1);
        return new GiftConfigCache.GiftBox(giftId, 1, 1, List.of(item));
    }

    /** Create a RandGift (type=2) box with one item */
    private GiftConfigCache.GiftBox randBox(long giftId, long itemId, long count) {
        var item = new GiftConfigCache.GiftItem(itemId, count, 10);
        return new GiftConfigCache.GiftBox(giftId, 2, 1, List.of(item));
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(giftService, "defaultBagType", 1);
    }

    // =========================================================
    // info
    // =========================================================
    @Nested
    @DisplayName("info()")
    class Info {

        @Test
        @DisplayName("TC-GIFT-001 [P] Lay thong tin qua tang ton tai – tra ve GiftInfoResp voi pool")
        void info_found_returnsGiftInfo() {
            GiftConfigCache.GiftBox box = defBox(100L, 201L, 5L);
            given(cfg.compile()).willReturn(Map.of(100L, box));

            GiftDTOs.GiftInfoResp result = giftService.info(100L);

            assertThat(result.getGiftItemId()).isEqualTo(100L);
            assertThat(result.getGiftType()).isEqualTo(1);
            assertThat(result.getPool()).hasSize(1);
            assertThat(result.getPool().get(0).getItemId()).isEqualTo(201L);
        }

        @Test
        @DisplayName("TC-GIFT-002 [N] Qua tang khong ton tai – tra ve resp rong")
        void info_notFound_returnsEmptyResp() {
            given(cfg.compile()).willReturn(Map.of());

            GiftDTOs.GiftInfoResp result = giftService.info(999L);

            assertThat(result.getGiftItemId()).isEqualTo(999L);
            assertThat(result.getGiftType()).isEqualTo(0);
            assertThat(result.getPool()).isEmpty();
        }
    }

    // =========================================================
    // open
    // =========================================================
    @Nested
    @DisplayName("open()")
    class Open {

        private GiftDTOs.OpenReq req(long giftId, int count) {
            GiftDTOs.OpenReq r = new GiftDTOs.OpenReq();
            r.setGiftItemId(giftId);
            r.setRoleId("100");
            r.setCount(count);
            r.setBagType(1);
            return r;
        }

        @Test
        @DisplayName("TC-GIFT-003 [N] Qua tang khong ton tai – tra ve GIFT_NOT_FOUND")
        void open_giftNotFound_returnsError() {
            given(cfg.compile()).willReturn(Map.of());

            GiftDTOs.OpenResp result = giftService.open(req(999L, 1));

            assertThat(result.isOk()).isFalse();
            assertThat(result.getError()).isEqualTo("GIFT_NOT_FOUND");
        }

        @Test
        @DisplayName("TC-GIFT-004 [N] Tieu thu tui that bai – tra ve BAG_CONSUME_FAIL")
        void open_bagConsumeFails_returnsError() {
            GiftConfigCache.GiftBox box = defBox(100L, 201L, 1L);
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString())).willReturn(Map.of("201", Map.of("isVirtual", 0)));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(false);
            given(bagOkResp.error()).willReturn("BAG_CONSUME_FAIL");

            GiftDTOs.OpenResp result = giftService.open(req(100L, 1));

            assertThat(result.isOk()).isFalse();
            assertThat(result.getError()).contains("BAG_CONSUME_FAIL");
        }

        @Test
        @DisplayName("TC-GIFT-005 [N] Cong vi that bai sau khi tieu thu – tra ve WALLET_ADD_FAIL")
        void open_walletAddFails_returnsError() {
            // Virtual item → goes to wallet
            GiftConfigCache.GiftBox box = defBox(100L, 301L, 100L);
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString()))
                    .willReturn(Map.of("301", Map.of("isVirtual", 1)));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(true);
            given(walletFeign.batchAdd(any())).willReturn(walletResult);
            given(walletResult.getCode()).willReturn(1); // failure

            GiftDTOs.OpenResp result = giftService.open(req(100L, 1));

            assertThat(result.isOk()).isFalse();
            assertThat(result.getError()).contains("WALLET_ADD_FAIL");
        }

        @Test
        @DisplayName("TC-GIFT-006 [N] Them vao tui that bai – tra ve BAG_ADD_FAIL")
        void open_bagAddFails_returnsError() {
            GiftConfigCache.GiftBox box = defBox(100L, 201L, 1L);
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString()))
                    .willReturn(Map.of("201", Map.of("isVirtual", 0)));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(true);
            given(bagFeign.add(any())).willReturn(bagAddItemResp);
            given(bagAddItemResp.isOk()).willReturn(false);
            given(bagAddItemResp.error()).willReturn("BAG_ADD_FAIL");

            GiftDTOs.OpenResp result = giftService.open(req(100L, 1));

            assertThat(result.isOk()).isFalse();
            assertThat(result.getError()).contains("BAG_ADD_FAIL");
        }

        @Test
        @DisplayName("TC-GIFT-007 [P] Mo qua DefGift thanh cong – cong item vao tui")
        void open_defGiftSuccess_addsItemsToBag() {
            GiftConfigCache.GiftBox box = defBox(100L, 201L, 3L);
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString()))
                    .willReturn(Map.of("201", Map.of("isVirtual", 0)));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(true);
            given(bagFeign.add(any())).willReturn(bagAddItemResp);
            given(bagAddItemResp.isOk()).willReturn(true);

            GiftDTOs.OpenResp result = giftService.open(req(100L, 1));

            assertThat(result.isOk()).isTrue();
            assertThat(result.getConsumed()).isEqualTo(1);
            assertThat(result.getBagItems()).hasSize(1);
        }

        @Test
        @DisplayName("TC-GIFT-007A [P] Mo qua gui quality va bagType xuong bag-service")
        void open_defGiftSuccess_propagatesQualityAndBagType() {
            GiftConfigCache.GiftBox box = defBox(100L, 201L, 3L);
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString()))
                    .willReturn(Map.of("201", Map.of("isVirtual", 0, "quality", 5, "bag_type", 9)));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(true);
            given(bagFeign.add(any())).willReturn(bagAddItemResp);
            given(bagAddItemResp.isOk()).willReturn(true);

            GiftDTOs.OpenReq req = req(100L, 1);
            req.setBagType(3);
            giftService.open(req);

            ArgumentCaptor<org.SouthMillion.dto.bag.BagAddItemReq> captor = ArgumentCaptor.forClass(org.SouthMillion.dto.bag.BagAddItemReq.class);
            then(bagFeign).should().add(captor.capture());
            var item = captor.getValue().getItems().getFirst();
            assertThat(item.getQuality()).isEqualTo(5);
            assertThat(item.getBagType()).isEqualTo(3);
        }

        @Test
        @DisplayName("TC-GIFT-008 [P] Mo qua co virtual item – cong ca vi lan tui")
        void open_mixedItems_addsToWalletAndBag() {
            // Two items: one virtual, one bag
            var itemVirtual = new GiftConfigCache.GiftItem(301L, 100L, 1);
            var itemBag     = new GiftConfigCache.GiftItem(201L, 1L, 1);
            var box = new GiftConfigCache.GiftBox(100L, 1, 1, List.of(itemVirtual, itemBag));
            given(cfg.compile()).willReturn(Map.of(100L, box));
            given(itemMetaFeign.batchMeta(anyString())).willReturn(Map.of(
                    "301", Map.of("isVirtual", 1),
                    "201", Map.of("isVirtual", 0)
            ));
            given(bagFeign.consume(any())).willReturn(bagOkResp);
            given(bagOkResp.isOk()).willReturn(true);
            given(walletFeign.batchAdd(any())).willReturn(walletResult);
            given(walletResult.getCode()).willReturn(0);
            given(walletResult.getData()).willReturn(mutateResp);
            given(mutateResp.ok()).willReturn(true);
            given(mutateResp.newBalances()).willReturn(Map.of(301L, 1100L));
            given(bagFeign.add(any())).willReturn(bagAddItemResp);
            given(bagAddItemResp.isOk()).willReturn(true);

            GiftDTOs.OpenResp result = giftService.open(req(100L, 1));

            assertThat(result.isOk()).isTrue();
            assertThat(result.getWalletAdds()).containsKey(301L);
            assertThat(result.getBagItems()).hasSize(1);
        }
    }
}
