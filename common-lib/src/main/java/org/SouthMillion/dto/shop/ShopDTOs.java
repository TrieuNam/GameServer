package org.SouthMillion.dto.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public class ShopDTOs {

    public enum Kind { COMMON, CLOTH, SHENMI }

    public record ListCommonReq(
            @NotBlank String roleId,
            @Min(1) int level,
            @Min(0) int page,
            @Min(0) int shopType // nếu file có "page_1" hay "shop_type", map vào đây
    ) {}

    public record ListClothReq(
            @NotBlank String roleId,
            @Min(1) int level,
            @Min(0) int page
    ) {}

    public record BuyReq(
            @NotBlank String roleId,
            @NotNull Kind kind,
            @Min(0) int indexOrSeq, // COMMON: index; CLOTH: seq; SHENMI: index
            @Min(1) int num,
            @Min(0) int receiveBagType,
            @Min(0) int walletBagType
    ) {}

    public record ShopItem(
            String idOrIndex, // text để hiển thị (index/seq/id)
            String name,
            long priceItemId,
            long priceNum,
            long rewardItemId,
            long rewardNum,
            int levelMin,
            int levelMax
    ) {}

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfoResp {
        private int mysterySlots;
        private long nextResetEpoch;
        private ShopListResp mystery; // reuse list cấu trúc hiện có
    }

    public record ShopListResp(List<ShopItem> items) {}

    public record BuyResp(boolean ok, String error) {}
}