package com.SouthMillion.crafting_service.service;

import com.SouthMillion.crafting_service.client.BagGrpcClient;
import com.SouthMillion.crafting_service.client.WalletGrpcClient;
import com.SouthMillion.crafting_service.entity.CraftingRecipe;
import com.SouthMillion.crafting_service.entity.UserCrafting;
import com.SouthMillion.crafting_service.repository.CraftingRecipeRepository;
import com.SouthMillion.crafting_service.repository.UserCraftingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.crafting.CraftingDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CraftingService Tests")
class CraftingServiceTest {

    @Mock private CraftingRecipeRepository recipeRepo;
    @Mock private UserCraftingRepository   userCraftingRepo;
    /** gRPC client cho bag-service */
    @Mock private BagGrpcClient            bagGrpcClient;
    /** gRPC client cho wallet-service */
    @Mock private WalletGrpcClient         walletGrpcClient;
    /** Real ObjectMapper để parseMaterials JSON hoạt động đúng */
    @Spy  private ObjectMapper             objectMapper = new ObjectMapper();

    @InjectMocks private CraftingService craftingService;

    private static CraftingRecipe recipe(Integer id, boolean enabled, String materialsJson,
                                         Long coinCost, long craftTime) {
        CraftingRecipe r = new CraftingRecipe();
        r.setRecipeId(id);
        r.setEnabled(enabled);
        r.setMaterialsJson(materialsJson);
        r.setCoinCost(coinCost);
        r.setCraftTime(craftTime);
        r.setResultItemId(100);
        r.setResultAmount(1);
        r.setRecipeName("Test Recipe");
        r.setRequiredLevel(1);
        return r;
    }

    // =========================================================
    // getRecipes
    // =========================================================
    @Nested
    @DisplayName("getRecipes()")
    class GetRecipes {

        @Test
        @DisplayName("TC-CRAFT-001 [P] Lay cong thuc theo level – tra ve danh sach co loc")
        void getRecipes_withLevel_returnsFiltered() {
            CraftingRecipe r = recipe(1, true, null, 0L, 60L);
            given(recipeRepo.findByRequiredLevelLessThanEqualAndEnabledTrue(10))
                    .willReturn(List.of(r));

            // roleId="role1" → parseLong fails → bagGrpcClient.getItemCounts KHÔNG được gọi
            List<CraftingDTOs.RecipeInfo> result = craftingService.getRecipes("role1", 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipeId()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-CRAFT-002 [P] Lay tat ca cong thuc khi khong co level – tra ve danh sach day du")
        void getRecipes_noLevel_returnsAll() {
            CraftingRecipe r1 = recipe(6, true, null, 0L, 30L);
            CraftingRecipe r2 = recipe(7, true, null, 0L, 60L);
            given(recipeRepo.findByEnabledTrue()).willReturn(List.of(r1, r2));

            List<CraftingDTOs.RecipeInfo> result = craftingService.getRecipes("role1", null);

            assertThat(result).hasSize(2);
        }
    }

    // =========================================================
    // startCraft
    // =========================================================
    @Nested
    @DisplayName("startCraft()")
    class StartCraft {

        @Test
        @DisplayName("TC-CRAFT-003 [N] Cong thuc khong ton tai – tra ve that bai")
        void startCraft_recipeNotFound_returnsFailure() {
            given(recipeRepo.findByRecipeId(999)).willReturn(Optional.empty());
            CraftingDTOs.CraftRequest req = new CraftingDTOs.CraftRequest();
            req.setRecipeId(999);
            req.setRoleId("role1");
            req.setCount(1);

            CraftingDTOs.CraftResponse result = craftingService.startCraft(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Recipe not found");
        }

        @Test
        @DisplayName("TC-CRAFT-004 [N] Cong thuc bi tat – tra ve that bai")
        void startCraft_recipeDisabled_returnsFailure() {
            CraftingRecipe r = recipe(2, false, null, 0L, 60L);
            given(recipeRepo.findByRecipeId(2)).willReturn(Optional.of(r));
            CraftingDTOs.CraftRequest req = new CraftingDTOs.CraftRequest();
            req.setRecipeId(2);
            req.setRoleId("role1");

            CraftingDTOs.CraftResponse result = craftingService.startCraft(req);

            assertThat(result.getSuccess()).isFalse();
        }

        @Test
        @DisplayName("TC-CRAFT-005 [P] Che tao thanh cong – khong co nguyen lieu, khong ton coin")
        void startCraft_noMaterialsNoCoin_success() {
            CraftingRecipe r = recipe(3, true, null, 0L, 120L);
            given(recipeRepo.findByRecipeId(3)).willReturn(Optional.of(r));
            given(userCraftingRepo.save(any(UserCrafting.class)))
                    .willAnswer(inv -> {
                        UserCrafting c = inv.getArgument(0);
                        c.setId(1L);
                        return c;
                    });
            CraftingDTOs.CraftRequest req = new CraftingDTOs.CraftRequest();
            req.setRecipeId(3);
            req.setRoleId("100");
            req.setCount(1);

            CraftingDTOs.CraftResponse result = craftingService.startCraft(req);

            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).contains("Crafting started");
            assertThat(result.getCraftingId()).isEqualTo(1L);
            // Xac nhan khong goi gRPC khi khong co nguyen lieu va coin = 0
            then(bagGrpcClient).shouldHaveNoInteractions();
            then(walletGrpcClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-CRAFT-006 [N] Nguyen lieu khong du – tra ve that bai")
        void startCraft_insufficientMaterials_returnsFailure() {
            String materialsJson = "[{\"itemId\":101,\"amount\":5}]";
            CraftingRecipe r = recipe(4, true, materialsJson, 0L, 60L);
            given(recipeRepo.findByRecipeId(4)).willReturn(Optional.of(r));
            // Mock gRPC: bagGrpcClient.hasItems trả false (không đủ nguyên liệu)
            given(bagGrpcClient.hasItems(anyLong(), any())).willReturn(false);

            CraftingDTOs.CraftRequest req = new CraftingDTOs.CraftRequest();
            req.setRecipeId(4);
            req.setRoleId("100");
            req.setCount(1);

            CraftingDTOs.CraftResponse result = craftingService.startCraft(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Not enough materials");
        }

        @Test
        @DisplayName("TC-CRAFT-007 [N] Coin khong du – tra ve that bai")
        void startCraft_insufficientCoins_returnsFailure() {
            CraftingRecipe r = recipe(5, true, null, 500L, 60L);
            given(recipeRepo.findByRecipeId(5)).willReturn(Optional.of(r));
            // Mock gRPC: walletGrpcClient.deductCoin trả false (không đủ coin)
            given(walletGrpcClient.deductCoin(anyLong(), anyLong(), anyLong(), anyInt())).willReturn(false);

            CraftingDTOs.CraftRequest req = new CraftingDTOs.CraftRequest();
            req.setRecipeId(5);
            req.setRoleId("100");
            req.setCount(1);

            CraftingDTOs.CraftResponse result = craftingService.startCraft(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Not enough coins");
        }
    }

    // =========================================================
    // getStatus
    // =========================================================
    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("TC-CRAFT-008 [P] Lay trang thai che tao – da hoan thanh co the nhan")
        void getStatus_completed_canClaim() {
            UserCrafting c = new UserCrafting();
            c.setId(1L);
            c.setRoleId("role1");
            c.setRecipeId(6);
            c.setStatus("IN_PROGRESS");
            c.setStartTime(Instant.now().minusSeconds(200));
            c.setEndTime(Instant.now().minusSeconds(10)); // already past
            given(userCraftingRepo.findByRoleId("role1")).willReturn(List.of(c));

            List<CraftingDTOs.CraftingStatus> result = craftingService.getStatus("role1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCanClaim()).isTrue();
        }

        @Test
        @DisplayName("TC-CRAFT-009 [P] Lay trang thai che tao – chua hoan thanh")
        void getStatus_notCompleted_cannotClaim() {
            UserCrafting c = new UserCrafting();
            c.setId(2L);
            c.setRoleId("role1");
            c.setRecipeId(7);
            c.setStatus("IN_PROGRESS");
            c.setStartTime(Instant.now());
            c.setEndTime(Instant.now().plusSeconds(300)); // still in future
            given(userCraftingRepo.findByRoleId("role1")).willReturn(List.of(c));

            List<CraftingDTOs.CraftingStatus> result = craftingService.getStatus("role1");

            assertThat(result.get(0).getCanClaim()).isFalse();
        }
    }

    // =========================================================
    // claim
    // =========================================================
    @Nested
    @DisplayName("claim()")
    class Claim {

        @Test
        @DisplayName("TC-CRAFT-010 [N] Phien che tao khong ton tai – tra ve that bai")
        void claim_notFound_returnsFailure() {
            given(userCraftingRepo.findById(999L)).willReturn(Optional.empty());
            CraftingDTOs.ClaimRequest req = new CraftingDTOs.ClaimRequest();
            req.setCraftingId(999L);
            req.setRoleId("role1");

            CraftingDTOs.ClaimResponse result = craftingService.claim(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Crafting not found");
        }

        @Test
        @DisplayName("TC-CRAFT-011 [N] Khong phai phien cua nguoi dung nay – tra ve that bai")
        void claim_wrongRole_returnsFailure() {
            UserCrafting c = new UserCrafting();
            c.setId(1L);
            c.setRoleId("role1");
            c.setStatus("IN_PROGRESS");
            given(userCraftingRepo.findById(1L)).willReturn(Optional.of(c));
            CraftingDTOs.ClaimRequest req = new CraftingDTOs.ClaimRequest();
            req.setCraftingId(1L);
            req.setRoleId("role_OTHER");

            CraftingDTOs.ClaimResponse result = craftingService.claim(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Not your crafting");
        }

        @Test
        @DisplayName("TC-CRAFT-012 [N] Trang thai khong phai IN_PROGRESS – tra ve that bai")
        void claim_alreadyClaimed_returnsFailure() {
            UserCrafting c = new UserCrafting();
            c.setId(2L);
            c.setRoleId("role1");
            c.setStatus("CLAIMED");
            given(userCraftingRepo.findById(2L)).willReturn(Optional.of(c));
            CraftingDTOs.ClaimRequest req = new CraftingDTOs.ClaimRequest();
            req.setCraftingId(2L);
            req.setRoleId("role1");

            CraftingDTOs.ClaimResponse result = craftingService.claim(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Already claimed");
        }

        @Test
        @DisplayName("TC-CRAFT-013 [N] Che tao chua hoan thanh – tra ve that bai")
        void claim_notCompleted_returnsFailure() {
            UserCrafting c = new UserCrafting();
            c.setId(3L);
            c.setRoleId("role1");
            c.setStatus("IN_PROGRESS");
            c.setEndTime(Instant.now().plusSeconds(600)); // not done yet
            given(userCraftingRepo.findById(3L)).willReturn(Optional.of(c));
            CraftingDTOs.ClaimRequest req = new CraftingDTOs.ClaimRequest();
            req.setCraftingId(3L);
            req.setRoleId("role1");

            CraftingDTOs.ClaimResponse result = craftingService.claim(req);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("not completed yet");
        }

        @Test
        @DisplayName("TC-CRAFT-014 [P] Nhan thuong thanh cong – danh dau CLAIMED")
        void claim_success_marksAsClaimed() {
            UserCrafting c = new UserCrafting();
            c.setId(4L);
            c.setRoleId("role1");
            c.setStatus("IN_PROGRESS");
            c.setEndTime(Instant.now().minusSeconds(10)); // done
            c.setRecipeId(99);
            // Recipe không tồn tại → rewards rỗng → không gọi bagGrpcClient
            given(userCraftingRepo.findById(4L)).willReturn(Optional.of(c));
            given(recipeRepo.findByRecipeId(99)).willReturn(Optional.empty());
            given(userCraftingRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            CraftingDTOs.ClaimRequest req = new CraftingDTOs.ClaimRequest();
            req.setCraftingId(4L);
            req.setRoleId("role1");

            CraftingDTOs.ClaimResponse result = craftingService.claim(req);

            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).contains("Claimed");
            then(bagGrpcClient).shouldHaveNoInteractions();
        }
    }
}
