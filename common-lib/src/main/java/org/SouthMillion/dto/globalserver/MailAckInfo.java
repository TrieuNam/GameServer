package org.SouthMillion.dto.globalserver;

import lombok.Data;
import org.SouthMillion.proto.msgmail.Msgmail;

@Data
public class MailAckInfo {
    private Integer mailType;
    private Integer mailIndex;
    private Integer ret; // 0: ok, khác là lỗi

    public Msgmail.PB_MailAckInfo toProto() {
        return Msgmail.PB_MailAckInfo.newBuilder()
                .setMailType(mailType == null ? 0 : mailType)
                .setMailIndex(mailIndex == null ? 0 : mailIndex)
                .setRet(ret == null ? 0 : ret)
                .build();
    }
}