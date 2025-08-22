package org.SouthMillion.dto.equip;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/**
 * DTO cho tính năng "Equip Fumo" (附魔) — ánh xạ với proto:
 *   message PB_EquipFuMoData { int32 level; int32 exp; uint32 end_time; }
 *   message PB_SCEquipFuMoListInfo { repeated PB_EquipFuMoData fumo_list; }
 *   message PB_SCEquipFuMoOneInfo  { int32 equip_type; PB_EquipFuMoData fumo_data; }
 *
 * Gợi ý REST trong equip-service:
 *   GET  /api/equip/fumo/{roleId}                 -> FumoListResp
 *   GET  /api/equip/fumo/{roleId}/{equipType}     -> FumoOneResp
 *   POST /api/equip/fumo/add-exp                  -> AddExpReq -> FumoOneResp
 *   POST /api/equip/fumo/activate                 -> ActivateReq -> FumoOneResp
 *   POST /api/equip/fumo/reset                    -> ResetReq -> OkResp
 */
public final class EquipFumoDTOs {

    /** Thông tin Fumo của 1 ô trang bị (không kèm equipType, dùng chung cho list/one). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FumoData(
            @Min(0) int level,
            @Min(0) int exp,
            /**
             * epoch seconds (uint32 bên proto) — dùng long để an toàn.
             * 0 nghĩa là không có hiệu lực/không đếm giờ.
             */
            @Min(0) long endTimeEpochSec
    ) {}

    /** Trả về toàn bộ danh sách Fumo (tuỳ bạn định nghĩa: hoặc là theo thứ tự slot/equipType 1..N). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FumoListResp(
            @NotNull List<FumoData> fumoList
    ) {}

    /** Trả về Fumo của 1 ô trang bị (theo equipType). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FumoOneResp(
            @Min(0) int equipType,
            FumoData fumoData
    ) {}

    // ================== Request DTOs cho thao tác Fumo ==================

    /**
     * Cộng EXP Fumo cho 1 ô trang bị.
     * Server có thể xử lý tiêu hao vật phẩm trong costItems trước, sau đó cộng exp, tính level/endTime mới.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddExpReq(
            @NotBlank String roleId,
            @Min(0) int equipType,
            /** EXP cộng thêm (server có thể giới hạn, ví dụ <= 1e9). */
            @Min(1) int addExp,
            /**
             * Danh sách vật phẩm tiêu hao: itemId -> số lượng.
             * Tuỳ logic server (có thể rỗng nếu addExp không yêu cầu vật phẩm).
             */
            Map<@Min(1) Integer, @Min(1) Long> costItems
    ) {}

    /**
     * Kích hoạt/đặt endTime cho 1 ô trang bị (ví dụ buff hiệu lực đến thời điểm nào).
     * Nếu logic của bạn là "bật Fumo trong X giây" thì service có thể tự set endTimeEpochSec = now + duration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActivateReq(
            @NotBlank String roleId,
            @Min(0) int equipType,
            /** epoch giây khi hiệu lực Fumo kết thúc (0 = remove/clear). */
            @Min(0) long endTimeEpochSec
    ) {}

    /** Reset/xoá Fumo của 1 ô trang bị (tùy tính năng). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResetReq(
            @NotBlank String roleId,
            @Min(0) int equipType,
            /** Vật phẩm/chi phí reset nếu có. */
            Map<@Min(1) Integer, @Min(1) Long> costItems
    ) {}

    /** Response OK/Fail đơn giản cho các thao tác không cần trả dữ liệu kèm. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OkResp(
            boolean ok,
            String message
    ) {
        public static OkResp OK() { return new OkResp(true, null); }
        public static OkResp NG(String msg) { return new OkResp(false, msg); }
    }
}