package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;


/**
 * DTOs dùng cho bag-service (inventory) — bản POJO, không dùng record.
 */
public final class BagDTOs {

    /**
     * Một slot trong túi.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagSlotView {
        @Min(0) private int slotIndex;
        @Min(0) private int itemId;
        @Min(0) private long count;
        @Min(0) private Long expireAtEpochSec; // nullable
        private Boolean bound;                 // nullable
    }

    /**
     * Ảnh chiếu toàn bộ túi.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagView {
        @NotBlank private String roleId;
        @Min(0)   private byte bagType;
        @Min(0)   private int capacity;
        @Min(0)   private int used;
        @NotNull  @Size(min = 0) private List<BagSlotView> slots;
    }

    /**
     * Mô tả thay đổi item (cộng/trừ).
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemDelta {
        @Min(1) private int itemId;
        @Min(1) private long count;
        private boolean bound;
        private String reason;
        @Min(0) private Long expireAtEpochSec; // nullable
        /** nếu true => bỏ qua pileLimit/meta, dồn vào 1 stack đúng bằng 'count' */
        private Boolean singleStack;           // nullable

        /** ctor rút gọn giữ tương thích cũ */
        public ItemDelta(int itemId, long count, boolean bound, String reason) {
            this.itemId = itemId;
            this.count = count;
            this.bound = bound;
            this.reason = reason;
        }

    }

    /**
     * Yêu cầu cộng item vào túi.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddItemReq {
        @NotBlank private String roleId;
        @Min(0)   private byte bagType;
        @NotNull  @Size(min = 1) private List<ItemDelta> items;
        @Min(0)   private int srcMsgId;
        @Min(0)   private int srcOp;
    }

    /**
     * Kết quả add.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddItemResp {
        @NotNull private Map<Integer, Long> added;
        private Map<Integer, Long> overflow; // nullable
        private String message;              // nullable
    }

    /**
     * Yêu cầu tiêu hao item trong túi.
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConsumeReq {
        @NotBlank private String roleId;
        @Min(0)   private byte bagType;
        @NotNull  @Size(min = 1) private List<ItemDelta> items;
        @Min(0)   private int srcMsgId;
        @Min(0)   private int srcOp;
    }

    /**
     * Phản hồi OK/Fail đơn giản.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OkResp {
        private boolean ok;
        private String message; // nullable

        public static OkResp OK()              { return new OkResp(true, null); }
        public static OkResp NG(String msg)    { return new OkResp(false, msg); }

        /** Giữ tương thích với chỗ gọi cũ: resp.ok() / resp.message() */
        public boolean ok()        { return ok; }
        public String  message()   { return message; }
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SortReq {
        @NotBlank private String roleId;
        @Min(0)   private byte bagType;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExpandReq {
        @NotBlank private String roleId;
        @Min(0)   private byte bagType;
        /** số slot muốn mở thêm */
        @Min(1)   private int slots;
    }
}