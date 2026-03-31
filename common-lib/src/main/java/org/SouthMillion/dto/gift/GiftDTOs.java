package org.SouthMillion.dto.gift;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Map;

public class GiftDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OpenReq {
        @NotBlank
        private String roleId;

        /** itemId của hộp quà (gift.json:id) */
        @Min(1)
        private long giftItemId;

        /** số lần mở */
        @Min(1)
        private int count;

        /** loại túi để tiêu thụ hộp và nhận item (mặc định 1) */
        private int bagType;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RollItem {
        private long itemId;
        private long count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OpenResp {
        private boolean ok;
        private String error;

        /** tổng số hộp đã tiêu thụ */
        private long consumed;

        /** item cộng vào túi */
        private List<RollItem> bagItems;

        /** tiền tệ cộng vào ví: itemId -> amount */
        private Map<Long, Long> walletAdds;
    }

    /** Xem thông tin gift (preview) */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GiftInfoResp {
        private long giftItemId;
        private int giftType;     // 1 = DefGift (cố định), 2 = RandGift (ngẫu nhiên)
        private int randNum;      // số lần rút mỗi lần mở
        private List<RollItem> pool; // danh sách phần tử trong gift.json (itemId + count); rate không trả về để đơn giản
    }


}