package com.SouthMillion.webSocket_server.net;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.SouthMillion.proto.Msgserver.Msgserver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Emitters – tiện ích encode & push frame ra PlayerSession.
 * LƯU Ý: Class này dùng PacketCodec.encode(msgId, payload) của project bạn.
 */
@Slf4j
@UtilityClass
public class Emitters {

    /* =========================================================
     * Core emit helpers
     * ========================================================= */
    public static void emit(PlayerSession ps, int msgId, byte[] payload) {
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

    /* =========================================================
     * Login
     * ========================================================= */
    public static void sendLoginAck(PlayerSession ps, int result, int forbidSeconds) {
        var ack = Msglogin.PB_SCLoginToAccount.newBuilder()
                .setResult(result)
                .setForbidTime(Math.max(0, forbidSeconds))
                .build();
        emit(ps, MsgIds.SC_LOGIN_ACK, ack.toByteArray());
    }

    public static void sendAccountKeyError(PlayerSession ps) {
        var msg = Msglogin.PB_SCAccountKeyError.newBuilder().build();
        emit(ps, MsgIds.SC_ACCOUNT_KEY_ERR, msg.toByteArray());
    }

    /* =========================================================
     * Heartbeat
     * ========================================================= */
    public static void sendHeartbeatResp(PlayerSession ps) {
        var resp = Msgserver.PB_SCHeartbeatResp.newBuilder()
                .setReserve(0)
                .build();
        emit(ps, MsgIds.SC_HEARTBEAT_RESP, resp.toByteArray());
    }

    /* =========================================================
     * Time
     * ========================================================= */
    public static void sendTimeAck(PlayerSession ps, int serverEpochSec, int openDays) {
        var t = Msgserver.PB_SCTimeAck.newBuilder()
                .setServerTime(serverEpochSec)         // uint32 trong proto
                .setServerRealStartTime(0)
                .setOpenDays(openDays)
                .setServerRealCombineTime(0)
                .build();
        emit(ps, MsgIds.SC_TIME_ACK, t.toByteArray());
    }

    /* =========================================================
     * Disconnect
     * ========================================================= */
    public static void sendDisconnectNotice(PlayerSession ps, int reason) {
        int roleId = safeParseInt(ps != null ? ps.getRoleId() : null, 0);
        var b = Msgserver.PB_SCDisconnectNotice.newBuilder()
                .setReason(reason);
        if (roleId > 0) b.setRoleId(roleId);
        if (ps != null && ps.getUsername() != null && !ps.getUsername().isBlank()) {
            b.setUserName(ps.getUsername());
        }
        emit(ps, MsgIds.SC_DISCONNECT_NOTICE, b.build().toByteArray());
    }

    /* =========================================================
     * Role bootstrap (tối thiểu)
     * ========================================================= */
    public static void sendRoleInfoAck(PlayerSession ps, RoleDTOs.RoleResp r) {
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

    /* =========================================================
     * Bag / Knapsack (MỚI THÊM)
     * ========================================================= */

    /** Gửi toàn bộ hành trang – từ danh sách item view (item_id + num). */
    public static void sendKnapsackAllInfo(PlayerSession ps, List<BagDTOs.ItemView> list) {
        if (list == null) list = List.of();
        var b = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
        list.stream()
                .sorted(Comparator.comparing(BagDTOs.ItemView::getItemId))
                .forEach(iv -> b.addItemList(
                        Msgknapsack.PB_ItemData.newBuilder()
                                .setItemId(safeParseInt(iv.getItemId(), 0))
                                .setNum(safeParseLong(iv.getNum(), 0L))
                                .build()
                ));
        emit(ps, MsgIds.SC_KNAPSACK_ALL_INFO, b.build().toByteArray());
    }

    /** Gửi thông tin đơn lẻ 1 vật phẩm với số lượng hiện tại. */
    public static void sendKnapsackSingleInfo(PlayerSession ps, int itemId, long num) {
        var msg = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                .setItem(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(itemId)
                        .setNum(Math.max(0L, num))
                        .build())
                .build();
        emit(ps, MsgIds.SC_KNAPSACK_SINGLE_INFO, msg.toByteArray());
    }

    /** Gửi thông báo “số lượng vật phẩm không đủ”. */
    public static void sendItemNotEnoughNotice(PlayerSession ps, int itemId) {
        var msg = Msgknapsack.PB_SCItemNotEnoughNotice.newBuilder()
                .setItemId(itemId)
                .build();
        emit(ps, MsgIds.SC_ITEM_NOT_ENOUGH_NOTICE, msg.toByteArray());
    }

    /* =========================================================     * Wallet / Currency balance update
     * ========================================================= */

    /**
     * Push current wallet balances to the client as individual knapsack single-info
     * messages.  Currency items (gold, diamond, …) share the same itemId space as
     * regular bag items, so the client can update its local currency counters.
     *
     * @param ps        player session to push to
     * @param balances  map of (itemId → amount) from {@code WalletDTOs.BalancesResp}
     */
    public static void sendWalletBalances(PlayerSession ps, Map<Long, Long> balances) {
        if (ps == null || balances == null || balances.isEmpty()) return;
        for (Map.Entry<Long, Long> entry : balances.entrySet()) {
            int itemId = entry.getKey().intValue();
            long amount = entry.getValue() != null ? entry.getValue() : 0L;
            sendKnapsackSingleInfo(ps, itemId, amount);
        }
    }

    /**
     * Push a single currency balance to the client.
     * Convenience wrapper over {@link #sendKnapsackSingleInfo}.
     */
    public static void sendCurrencyUpdate(PlayerSession ps, long itemId, long balance) {
        sendKnapsackSingleInfo(ps, (int) itemId, balance);
    }

    /* =========================================================     * Helpers
     * ========================================================= */
    private static int safeParseInt(Object v, int def) {
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static long safeParseLong(Object v, long def) {
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static String nvl(String... ss) {
        if (ss == null) return "";
        for (String s : ss) if (s != null && !s.isBlank()) return s;
        return "";
    }
    private static ByteString bytes(String s) {
        if (s == null) s = "";
        return ByteString.copyFromUtf8(s);
    }
}