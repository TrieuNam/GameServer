package org.SouthMillion.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDTO {
    private Long id;
    private Integer type;         // 0: System Notice, 1: Item Not Enough, 2: SystemMsg, 3: ZeroHour, 4: RechargeRet
    private String content;       // Nội dung thông báo
    private Integer code;         // Mã lỗi/code (nếu có)
    private Integer itemId;       // id vật phẩm (nếu là item)
    private Long createdTime;     // Thời gian tạo
}