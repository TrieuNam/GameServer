package com.SouthMillion.shop_service.service;

import com.SouthMillion.shop_service.config.ShopConfigCache;
import com.SouthMillion.shop_service.entity.ShopLimit;
import com.SouthMillion.shop_service.repository.ShopLimitRepository;
import com.SouthMillion.shop_service.service.config.BagFeign;
import com.SouthMillion.shop_service.service.config.ItemMetaFeign;
import com.SouthMillion.shop_service.service.config.WalletFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
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
import java.util.Optional;

import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagAddItemResp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Tests")
class ShopServiceTest {

    @Mock
    private ShopConfigCache cfg;

    @Mock
    private BagFeign bagFeign;

    @Mock
    private ShopLimitRepository limitRepo;

    @Mock
    private WalletFeignClient walletFeign;

    @Mock
    private ItemMetaFeign itemMeta;

    @InjectMocks
    private ShopService shopService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inject @Value fields that are not set by @InjectMocks
        ReflectionTestUtils.setField(shopService, "tz", "UTC");
        ReflectionTestUtils.setField(shopService, "shenmiSlots", 6);
    }

    // =========================================================
    // buy – CONFIG_NOT_FOUND
    // =========================================================
    @Nested
    @DisplayName("buy()")
    class Buy {

        @Test
        @DisplayName("TC-SHOP-001 [N] Config khong tim thay – tra ve CONFIG_NOT_FOUND")
        void buy_configNotFound_returnsFail() throws Exception {
            JsonNode emptyArr = mapper.createArrayNode();
            given(cfg.common()).willReturn(emptyArr);

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("role-001", ShopDTOs.Kind.COMMON, 999, 1, 0, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isFalse();
            assertThat(result.error()).isEqualTo("CONFIG_NOT_FOUND");
        }

        @Test
        @DisplayName("TC-SHOP-002 [N] Vuot qua gioi han mua (FOREVER) – tra ve QUOTA_EXCEEDED")
        void buy_quotaExceeded_returnsFail() throws Exception {
            // Config: index=1, quota_type=2 (forever), param=5
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"quota_type\":2,\"param\":5," +
                    "\"exchange_item_id\":0,\"exchange_item_num\":0," +
                    "\"reward_item_id\":0,\"reward_num\":0}]"
            );
            given(cfg.common()).willReturn(config);

            // Already bought 5, trying to buy 1 more -> 6 > 5 -> QUOTA_EXCEEDED
            ShopLimit lim = new ShopLimit();
            lim.setCount(5);
            given(limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    any(), any(), anyInt(), any(), any())).willReturn(Optional.of(lim));

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("role-001", ShopDTOs.Kind.COMMON, 1, 1, 0, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isFalse();
            assertThat(result.error()).isEqualTo("QUOTA_EXCEEDED");
        }

        @Test
        @DisplayName("TC-SHOP-003 [P] Mua hang khong co chi phi va phan thuong – thanh cong")
        void buy_noCostNoReward_success() throws Exception {
            // Config: index=1, no quota, no cost (exchange_item_id=0), no reward (reward_item_id=0)
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"quota_type\":0,\"param\":0," +
                    "\"exchange_item_id\":0,\"exchange_item_num\":0," +
                    "\"reward_item_id\":0,\"reward_num\":0}]"
            );
            given(cfg.common()).willReturn(config);

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("role-001", ShopDTOs.Kind.COMMON, 1, 1, 0, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().isOk()).isTrue();
            then(walletFeign).shouldHaveNoInteractions();
            then(bagFeign).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-SHOP-004 [P] Mua hang qua CLOTH kind – tim dung config")
        void buy_clothKind_configFound() throws Exception {
            // Cloth config uses "seq" not "index"
            JsonNode config = mapper.readTree(
                    "[{\"seq\":1,\"quota_type\":0,\"param\":0," +
                    "\"exchange_item_id\":0,\"buy_item_num\":0," +
                    "\"item_id\":0,\"num\":0}]"
            );
            given(cfg.cloth()).willReturn(config);

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("role-001", ShopDTOs.Kind.CLOTH, 1, 1, 0, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isTrue();
        }

        @Test
        @DisplayName("TC-SHOP-005 [P] Mua hang voi FOREVER quota – ghi lai so lan mua")
        void buy_withForeverQuota_savesLimit() throws Exception {
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"quota_type\":2,\"param\":10," +
                    "\"exchange_item_id\":0,\"exchange_item_num\":0," +
                    "\"reward_item_id\":0,\"reward_num\":0}]"
            );
            given(cfg.common()).willReturn(config);

            // No existing limit -> already=0, after=1 <= 10 -> not exceeded
            given(limitRepo.findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
                    any(), any(), anyInt(), any(), any())).willReturn(Optional.empty());
            given(limitRepo.save(any(ShopLimit.class))).willAnswer(inv -> inv.getArgument(0));

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("role-001", ShopDTOs.Kind.COMMON, 1, 1, 0, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isTrue();
            then(limitRepo).should().save(any(ShopLimit.class)); // quota record saved
        }

        @Test
        @DisplayName("TC-SHOP-006 [P] Mua reward vao bag gui quality va receiveBagType")
        void buy_nonVirtualReward_propagatesQualityAndBagType() throws Exception {
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"quota_type\":0,\"param\":0," +
                    "\"exchange_item_id\":0,\"exchange_item_num\":0," +
                    "\"reward_item_id\":201,\"reward_num\":2}]"
            );
            given(cfg.common()).willReturn(config);
            given(itemMeta.batchMeta("201")).willReturn(Map.of("201", Map.of("isVirtual", 0, "quality", 8)));
            given(bagFeign.add(any(BagAddItemReq.class))).willReturn(BagAddItemResp.ok(List.of()));

            ShopDTOs.BuyReq req = new ShopDTOs.BuyReq("1001", ShopDTOs.Kind.COMMON, 1, 1, 6, 0);
            ResultDTO<ShopDTOs.BuyResp> result = shopService.buy(req);

            assertThat(result.ok()).isTrue();
            ArgumentCaptor<BagAddItemReq> captor = ArgumentCaptor.forClass(BagAddItemReq.class);
            then(bagFeign).should().add(captor.capture());
            BagAddItemReq.Item item = captor.getValue().getItems().getFirst();
            assertThat(item.getQuality()).isEqualTo(8);
            assertThat(item.getBagType()).isEqualTo(6);
        }
    }

    // =========================================================
    // listCommon
    // =========================================================
    @Nested
    @DisplayName("listCommon()")
    class ListCommon {

        @Test
        @DisplayName("TC-SHOP-010 [P] Lay danh sach hang hoa thuong – tra ve dung so luong")
        void listCommon_returnsMatchingItems() throws Exception {
            JsonNode config = mapper.readTree(
                    "[" +
                    "{\"page\":1,\"index\":1,\"level_min\":1,\"level_max\":100," +
                    "\"exchange_item_id\":1,\"exchange_item_num\":100," +
                    "\"reward_item_id\":2,\"reward_num\":1}," +
                    "{\"page\":2,\"index\":2,\"level_min\":1,\"level_max\":100," +
                    "\"exchange_item_id\":1,\"exchange_item_num\":200," +
                    "\"reward_item_id\":3,\"reward_num\":1}" +
                    "]"
            );
            given(cfg.common()).willReturn(config);

            ShopDTOs.ListCommonReq req = new ShopDTOs.ListCommonReq("role-001", 50, 1, 0);
            ResultDTO<ShopDTOs.ShopListResp> result = shopService.listCommon(req);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().items()).hasSize(1); // only page=1
        }

        @Test
        @DisplayName("TC-SHOP-011 [P] Item bi gioi han level – chi tra ve item phu hop")
        void listCommon_levelFiltered_returnsOnlyEligible() throws Exception {
            JsonNode config = mapper.readTree(
                    "[" +
                    "{\"page\":1,\"index\":1,\"level_min\":1,\"level_max\":10," +
                    "\"exchange_item_id\":1,\"exchange_item_num\":100," +
                    "\"reward_item_id\":2,\"reward_num\":1}," +
                    "{\"page\":1,\"index\":2,\"level_min\":20,\"level_max\":50," +
                    "\"exchange_item_id\":1,\"exchange_item_num\":200," +
                    "\"reward_item_id\":3,\"reward_num\":1}" +
                    "]"
            );
            given(cfg.common()).willReturn(config);

            // Level 5 -> only first item matches (1-10), not second (20-50)
            ShopDTOs.ListCommonReq req = new ShopDTOs.ListCommonReq("role-001", 5, 1, 0);
            ResultDTO<ShopDTOs.ShopListResp> result = shopService.listCommon(req);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().items()).hasSize(1);
        }
    }

    // =========================================================
    // listMystery
    // =========================================================
    @Nested
    @DisplayName("listMystery()")
    class ListMystery {

        @Test
        @DisplayName("TC-SHOP-020 [P] Lay danh sach hang bi an – tra ve dung so slot")
        void listMystery_returnsSlotsItems() throws Exception {
            // 5 items in config, all match level 10
            JsonNode config = mapper.readTree(
                    "[" +
                    "{\"index\":1,\"buy_item\":1,\"buy_item_num\":100,\"item_id\":101,\"num\":1,\"level_min\":1,\"level_max\":100}," +
                    "{\"index\":2,\"buy_item\":1,\"buy_item_num\":200,\"item_id\":102,\"num\":1,\"level_min\":1,\"level_max\":100}," +
                    "{\"index\":3,\"buy_item\":1,\"buy_item_num\":300,\"item_id\":103,\"num\":1,\"level_min\":1,\"level_max\":100}," +
                    "{\"index\":4,\"buy_item\":1,\"buy_item_num\":400,\"item_id\":104,\"num\":1,\"level_min\":1,\"level_max\":100}," +
                    "{\"index\":5,\"buy_item\":1,\"buy_item_num\":500,\"item_id\":105,\"num\":1,\"level_min\":1,\"level_max\":100}" +
                    "]"
            );
            given(cfg.shenmi()).willReturn(config);

            // slotsOverride = 3 -> pick 3 random from 5 candidates
            ResultDTO<ShopDTOs.ShopListResp> result = shopService.listMystery(10, 3);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().items()).hasSize(3);
        }

        @Test
        @DisplayName("TC-SHOP-021 [P] Level khong du dieu kien – tra ve danh sach rong")
        void listMystery_levelNotEligible_returnsEmpty() throws Exception {
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"buy_item\":1,\"buy_item_num\":100,\"item_id\":101,\"num\":1," +
                    "\"level_min\":50,\"level_max\":100}]"
            );
            given(cfg.shenmi()).willReturn(config);

            // level = 10, item requires level 50+
            ResultDTO<ShopDTOs.ShopListResp> result = shopService.listMystery(10, 6);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().items()).isEmpty();
        }
    }

    // =========================================================
    // refreshMystery
    // =========================================================
    @Nested
    @DisplayName("refreshMystery()")
    class RefreshMystery {

        @Test
        @DisplayName("TC-SHOP-025 [P] Lam moi hang bi an – tra ve danh sach moi")
        void refreshMystery_returnsNewList() throws Exception {
            JsonNode config = mapper.readTree(
                    "[{\"index\":1,\"buy_item\":1,\"buy_item_num\":100,\"item_id\":101,\"num\":1," +
                    "\"level_min\":1,\"level_max\":100}]"
            );
            given(cfg.shenmi()).willReturn(config);

            ResultDTO<ShopDTOs.ShopListResp> result = shopService.refreshMystery("role-001", 10, 1);

            assertThat(result.ok()).isTrue();
            assertThat(result.data().items()).hasSize(1);
        }
    }
}
