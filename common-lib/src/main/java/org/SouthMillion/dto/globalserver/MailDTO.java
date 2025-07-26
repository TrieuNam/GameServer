package org.SouthMillion.dto.globalserver;

import com.google.protobuf.ByteString;
import lombok.*;
import org.SouthMillion.dto.ItemDTO;
import org.SouthMillion.proto.msgmail.Msgmail;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MailDTO {
    private Integer mailType;
    private Integer mailIndex;
    private Long recvTime;
    private Integer isRead;
    private Integer isFetch;
    private String subject;
    private String content;
    private List<ItemDTO> itemData;

    public Msgmail.PB_MailBriefData toProto() {
        Msgmail.PB_MailBriefData.Builder builder = Msgmail.PB_MailBriefData.newBuilder()
                .setMailType(mailType == null ? 0 : mailType)
                .setMailIndex(mailIndex == null ? 0 : mailIndex)
                .setRecvTime(recvTime == null ? 0 : recvTime.intValue())
                .setIsRead(isRead == null ? 0 : isRead)
                .setIsFetch(isFetch == null ? 0 : isFetch)
                .setSubject(ByteString.copyFromUtf8(subject == null ? "" : subject));
        if (itemData != null) {
            builder.addAllItemData(itemData.stream().map(ItemDTO::toProto).collect(Collectors.toList()));
        }
        return builder.build();
    }

    public Msgmail.PB_SCMailDetail toProtoDetail() {
        Msgmail.PB_SCMailDetail.Builder builder = Msgmail.PB_SCMailDetail.newBuilder()
                .setMailType(mailType == null ? 0 : mailType)
                .setMailIndex(mailIndex == null ? 0 : mailIndex)
                .setSubject(ByteString.copyFromUtf8(subject == null ? "" : subject))
                .setContenttxt(ByteString.copyFromUtf8(content == null ? "" : content));
        if (itemData != null) {
            builder.addAllItemData(itemData.stream().map(ItemDTO::toProto).collect(Collectors.toList()));
        }
        return builder.build();
    }
}