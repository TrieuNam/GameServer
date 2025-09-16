package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

public final class BagDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GrantItem {
        @NotNull private Integer itemId;
        @Min(1)  private Integer num;
        private Boolean bind;
        private Instant expireAt;   // giữ Instant
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GrantReq {  // <- thêm 'static'
        @NotBlank private String userId;
        @NotBlank private String roleId;
        @NotEmpty private List<BagDTOs.GrantItem> items;
        private String eventId;
    }


    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UseItemReq {
        @NotNull private Integer itemId;
        @Min(1)  private Integer num;
        private Integer param;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SellItemReq {
        @NotNull private Integer itemId;
        @Min(1)  private Integer num;
        private Long unitPrice;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemView {
        private String id;
        private String roleId;
        private Integer itemId;
        private Long num;           // <- đổi sang Long
        private Boolean bind;
        private Instant expireAt;   // <- giữ Instant
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SellResult {
        private Integer itemId;
        private Integer num;
        private Long goldGain;
    }
}
