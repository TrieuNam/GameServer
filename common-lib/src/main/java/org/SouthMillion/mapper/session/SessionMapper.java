package org.SouthMillion.mapper.session;

import org.SouthMillion.dto.session.DisconnectNoticeDTO;
import org.SouthMillion.dto.session.SessionHeartbeatDTO;
import org.SouthMillion.dto.session.TimeAckDTO;
import org.SouthMillion.proto.Msgserver.Msgserver;

import java.util.Optional;

public class SessionMapper {
    public static Msgserver.PB_SCHeartbeatResp toProto(SessionHeartbeatDTO dto) {
        return Msgserver.PB_SCHeartbeatResp.newBuilder()
                .setReserve(Optional.ofNullable(dto.getReserve()).orElse(0))
                .build();
    }

    public static Msgserver.PB_SCTimeAck toProto(TimeAckDTO dto) {
        return Msgserver.PB_SCTimeAck.newBuilder()
                .setServerTime(dto.getServerTime().intValue())
                .setServerRealStartTime(dto.getServerRealStartTime().intValue())
                .setOpenDays(dto.getOpenDays())
                .setServerRealCombineTime(dto.getServerRealCombineTime().intValue())
                .build();
    }

    public static Msgserver.PB_SCDisconnectNotice toProto(DisconnectNoticeDTO dto) {
        return Msgserver.PB_SCDisconnectNotice.newBuilder()
                .setReason(dto.getReason())
                .setRoleId(dto.getRoleId())
                .setUserName(dto.getUserName())
                .build();
    }
}