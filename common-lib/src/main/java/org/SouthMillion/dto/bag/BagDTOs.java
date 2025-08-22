package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs dùng cho bag-service (inventory).
 *
 * Phối hợp:
 * - Public API (ví dụ): GET /api/bag/{roleId}/{bagType} -> BagView
 * - Internal API:
 *      POST /internal/bag/add      -> AddItemReq   -> AddItemResp
 *      POST /internal/bag/consume  -> ConsumeReq   -> OkResp
 *
 * Ghi chú:
 * - bagType: 0 = túi thường; 1 = túi trang bị (tùy config hệ thống của bạn)
 * - srcMsgId/srcOp: nguồn gọi (để audit/log; ví dụ 1600=EQUIP, op=1/2/3...)
 */
public final class BagDTOs {

    /**
     * Một slot trong túi: chứa 1 loại item tại 1 chỉ số slot.
     * expireAtEpochSec: thời điểm hết hạn (epoch giây), có thể null nếu không có.
     * bound: cờ khóa/bind item (nếu hệ thống có khái niệm bind).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BagSlotView(
            @Min(0) int slotIndex,
            @Min(0) int itemId,
            @Min(0) long count,
            @Min(0) Long expireAtEpochSec,
            Boolean bound
    ) {}

    /**
     * Ảnh chiếu toàn bộ túi.
     * used: tổng số slot đang dùng; capacity: sức chứa tối đa (tính theo slot).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BagView(
            @NotBlank String roleId,
            @Min(0) byte bagType,
            @Min(0) int capacity,
            @Min(0) int used,
            @NotNull List<BagSlotView> slots
    ) {}

    /**
     * Mô tả thay đổi item (cộng/trừ) theo itemId và số lượng.
     * bound: đánh dấu bind nếu cần; reason: mô tả ngắn gọn (để log/audit).
     * expireAtEpochSec: nếu add item có hạn dùng (có thể null).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ItemDelta(
            @Min(1) int itemId,
            @Min(1) long count,
            boolean bound,
            String reason,
            @Min(0) Long expireAtEpochSec,
            /** NEW: nếu true => bỏ qua pileLimit/meta, dồn vào 1 stack đúng bằng 'count' */
            Boolean singleStack
    ) {
        /** ctor cũ vẫn dùng được, giữ tương thích */
        public ItemDelta(int itemId, long count, boolean bound, String reason) {
            this(itemId, count, bound, reason, null, null);
        }
    }

    /**
     * Yêu cầu cộng item vào túi (server sẽ cố gắng stack theo quy tắc pileLimit).
     * srcMsgId/srcOp: nguồn tác động (để trace). Có thể để 0 nếu không cần.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddItemReq(
            @NotBlank String roleId,
            @Min(0) byte bagType,
            @NotNull @Size(min = 1) List<ItemDelta> items,
            @Min(0) int srcMsgId,
            @Min(0) int srcOp
    ) {}

    /**
     * Kết quả add: added = số lượng thực sự thêm được theo từng itemId,
     * overflow = số lượng không thêm được (do đầy túi/pileLimit), có thể null/empty nếu không overflow.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddItemResp(
            @NotNull Map<Integer, Long> added,
            Map<Integer, Long> overflow,
            String message
    ) {}

    /**
     * Yêu cầu tiêu hao item trong túi.
     * Server phải đảm bảo tổng số lượng theo itemId đủ, nếu không trả về ok=false (hoặc báo chi tiết).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConsumeReq(
            @NotBlank String roleId,
            @Min(0) byte bagType,
            @NotNull @Size(min = 1) List<ItemDelta> items,
            @Min(0) int srcMsgId,
            @Min(0) int srcOp
    ) {}

    /**
     * Phản hồi OK/Fail đơn giản cho các thao tác không cần trả data kèm theo.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OkResp(
            boolean ok,
            String message
    ) {
        public static OkResp OK() { return new OkResp(true, null); }
        public static OkResp NG(String msg) { return new OkResp(false, msg); }
        public boolean ok() { return ok; }
        public String message() { return message; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SortReq(
            @NotBlank String roleId,
            @Min(0) byte bagType
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExpandReq(
            @NotBlank String roleId,
            @Min(0) byte bagType,
            /** số slot muốn mở thêm */
            @Min(1) int slots
    ) {}
}