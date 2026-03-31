package org.SouthMillion.dto.wallet;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.Map;

public class WalletDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Change {
        /**
         * id tiền tệ (virtual itemId)
         */
        private long itemId;
        /**
         * số lượng (>0). Với batchCost, amount là số cần trừ
         */
        private long amount;

        // Alias kiểu record
        public long itemId() {
            return itemId;
        }

        public long amount() {
            return amount;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchReq {
        @NotBlank
        private String roleId;

        @NotEmpty
        private List<Change> changes;

        /**
         * khóa idempotency (optional). Nếu cung cấp, đảm bảo không trừ/cộng 2 lần
         */
        private String idemKey;

        /**
         * mã reason (tự quy ước)
         */
        private Integer reason;

        /**
         * nhóm reason (tự quy ước)
         */
        private Integer reasonType;

        public BatchReq(@NotBlank String roleId, List<Change> walletAdds) {
            this.roleId = roleId;
            this.changes = walletAdds;
        }

        // Alias kiểu record
        public String roleId() {
            return roleId;
        }

        public List<Change> changes() {
            return changes;
        }

        public String idemKey() {
            return idemKey;
        }

        public Integer reason() {
            return reason;
        }

        public Integer reasonType() {
            return reasonType;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GetReq {
        @NotBlank
        private String roleId;

        @NotEmpty
        private List<Long> itemIds;

        // Alias kiểu record
        public String roleId() {
            return roleId;
        }

        public List<Long> itemIds() {
            return itemIds;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BalancesResp {
        private String roleId;
        /**
         * itemId -> balance
         */
        private Map<Long, Long> balances;
        private Long atEpochSec;

        // Alias kiểu record
        public String roleId() {
            return roleId;
        }

        public Map<Long, Long> balances() {
            return balances;
        }

        public Long atEpochSec() {
            return atEpochSec;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MutateResp {
        private boolean ok;
        private String error;
        /**
         * chỉ trả cho các item thay đổi (đã cộng/trừ)
         */
        private Map<Long, Long> newBalances;
        private Long atEpochSec;

        // Alias kiểu record (để code cũ gọi .ok()/.newBalances() không phải sửa)
        public boolean ok() {
            return ok;
        }

        public String error() {
            return error;
        }

        public Map<Long, Long> newBalances() {
            return newBalances;
        }

        public Long atEpochSec() {
            return atEpochSec;
        }
    }
}