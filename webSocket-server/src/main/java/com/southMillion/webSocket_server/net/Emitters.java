package com.southMillion.webSocket_server.net;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.southMillion.webSocket_server.dto.PlayerSession;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.SouthMillion.proto.Msgserver.Msgserver;

@Slf4j
@UtilityClass
public class Emitters {
    public void emit(PlayerSession ps, int msgId, byte[] payload) {
        if (ps == null || ps.getOutbound() == null) return;
        try {
            ps.getOutbound().tryEmitNext(PacketCodec.encode(msgId, payload));
        } catch (Throwable t) {
            log.warn("emit failed: msgId={}, ex={}", msgId, t.toString());
        }
    }
    public static void emit(PlayerSession ps, int msgId, MessageLite pb) {
        byte[] payload = pb != null ? pb.toByteArray() : new byte[0];
        byte[] frame = PacketCodec.encode(msgId, payload);
        ps.sendBinary(frame);
    }
    // ===== Login
    public void sendLoginAck(PlayerSession ps, int result, int forbidSeconds) {
        var ack = Msglogin.PB_SCLoginToAccount.newBuilder()
                .setResult(result)
                .setForbidTime(Math.max(0, forbidSeconds))
                .build();
        emit(ps, MsgIds.SC_LOGIN_ACK, ack.toByteArray());
    }

    public void sendAccountKeyError(PlayerSession ps) {
        var msg = Msglogin.PB_SCAccountKeyError
                .newBuilder().build();
        emit(ps, MsgIds.SC_ACCOUNT_KEY_ERR, msg.toByteArray());
    }

    // ===== Heartbeat
    public void sendHeartbeatResp(PlayerSession ps) {
        var resp = Msgserver.PB_SCHeartbeatResp.newBuilder()
                .setReserve(0)
                .build();
        emit(ps, MsgIds.SC_HEARTBEAT_RESP, resp.toByteArray());
    }

    // ===== Time
    public void sendTimeAck(PlayerSession ps, int serverEpochSec, int openDays) {
        var t = Msgserver.PB_SCTimeAck.newBuilder()
                .setServerTime(serverEpochSec)         // uint32 trong proto
                .setServerRealStartTime(0)
                .setOpenDays(openDays)
                .setServerRealCombineTime(0)
                .build();
        emit(ps, MsgIds.SC_TIME_ACK, t.toByteArray());
    }

    // ===== Disconnect
    public void sendDisconnectNotice(PlayerSession ps, int reason) {
        int roleId = safeParseInt(ps != null ? ps.getRoleId() : null, 0);
        var b = Msgserver.PB_SCDisconnectNotice.newBuilder()
                .setReason(reason);
        if (roleId > 0) b.setRoleId(roleId);
        if (ps != null && ps.getUsername() != null && !ps.getUsername().isBlank()) {
            b.setUserName(ps.getUsername());
        }
        emit(ps, MsgIds.SC_DISCONNECT_NOTICE, b.build().toByteArray());
    }

    // ===== Role bootstrap (tối thiểu)
    public void sendRoleInfoAck(PlayerSession ps, RoleDTOs.RoleResp r) {
        if (r == null) return;

        var role = Msgrole.PB_RoleInfo.newBuilder()
                .setRoleId(safeParseInt(r.getRoleId(), 0))             // proto: int32
                .setName(bytes(nvl(r.getName(), r.getNickname(), r.getRoleName(), "Player")))
                .setLevel(safeParseInt(r.getLevel(), 1))
                .setCap(safeParseLong(r.getCap(), 0L))
                .setHeadPicId(safeParseInt(r.getHeadPicId(), 0))
                .setTitleId(safeParseInt(r.getTitleId(), 0))
                .setGuildName(bytes(nvl(r.getGuildName())))
                .setKnightLevel(safeParseInt(r.getKnightLevel(), 0))
                .setHeadChar(bytes(nvl(r.getHeadChar())))
                .build();

        var ack = Msgrole.PB_SCRoleInfoAck.newBuilder()
                .setCurExp(safeParseLong(r.getCurExp(), 0L))
                .setCreateTime(safeParseLong(r.getCreateTimeEpochSec(), 0L))
                .setRoleinfo(role)
                // Appearance có thể để trống (optional)
                .build();

        emit(ps, MsgIds.SC_ROLE_INFO_ACK, ack.toByteArray());
    }

    // ===== Helpers
    private int safeParseInt(Object v, int def) {
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private long safeParseLong(Object v, long def) {
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private String nvl(String... ss) {
        if (ss == null) return "";
        for (String s : ss) if (s != null && !s.isBlank()) return s;
        return "";
    }
    private ByteString bytes(String s) {
        if (s == null) s = "";
        return ByteString.copyFromUtf8(s);
    }
}