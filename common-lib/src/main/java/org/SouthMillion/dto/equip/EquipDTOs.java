package org.SouthMillion.dto.equip;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public final class EquipDTOs {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipItem {
        private int equipType;
        private int itemId;
        private int hp;
        private int attack;
        private int defend;
        private int speed;
        private int attrType1;
        private int attrValue1;
        private int attrType2;
        private int attrValue2;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResp {
        private List<EquipItem> items;
    }

    /** Yêu cầu mặc trang bị */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipReq {
        @NotBlank
        private String roleId;

        @Min(1)
        private int itemId;

        /** 0=bag thường; 1=túi trang bị; null=theo cấu hình equip.equip-bag-type */
        private Byte bagType;

        /** optional — ép mặc vào slot cụ thể nếu meta không chỉ rõ (null = auto) */
        private Byte forceEquipType;
    }

    /** Yêu cầu tháo trang bị */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnequipReq {
        @NotBlank
        private String roleId;

        @Min(0)
        private int equipType;

        private Byte bagType;
    }

    /** Phản hồi OK/NG đơn giản */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OkResp {
        private boolean ok;
        private String message;

        public static OkResp OK() { return new OkResp(true, null); }
        public static OkResp NG(String msg) { return new OkResp(false, msg); }

        // Giữ tương thích với chỗ gọi resp.ok() / resp.message()
        public boolean ok() { return ok; }
        public String message() { return message; }
    }
}