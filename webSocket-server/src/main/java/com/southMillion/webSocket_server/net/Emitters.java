package com.southMillion.webSocket_server.net;

import com.southMillion.webSocket_server.dto.PlayerSession;
import io.micrometer.common.util.StringUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.SouthMillion.proto.Msgserver.Msgserver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

@Slf4j
@UtilityClass
public class Emitters {

    public void emit(PlayerSession ps, int msgId, byte[] payload) {
        if (ps == null || ps.getOutbound() == null) return;
        ps.getOutbound().tryEmitNext(PacketCodec.encode(msgId, payload));
    }

    public static void sendBoxEquipInfo(PlayerSession ps, BoxDTOs.EquipInfo info) {
        if (info == null) return;
        var data = info.getEquipInfo();
        if (data == null) data = new BoxDTOs.EquipRolled(); // tránh NPE

        var equipPb = Msgequip.PB_EquipData.newBuilder()
                .setEquipType(nvl(data.getEquipType()))
                .setItemId(nvl(data.getItemId()))
                .setHp(nvl(data.getHp()))
                .setAttack(nvl(data.getAttack()))
                .setDefend(nvl(data.getDefend()))
                .setSpeed(nvl(data.getSpeed()))
                .setAttrType1(nvl(data.getAttrType1()))
                .setAttrValue1(nvl(data.getAttrValue1()))
                .setAttrType2(nvl(data.getAttrType2()))
                .setAttrValue2(nvl(data.getAttrValue2()))
                .build();

        var msg = Msgbox.PB_SCBoxEquipInfo.newBuilder()
                .setIsNew(nvl(info.getIsNew()))
                .setEquipInfo(equipPb)
                .build();

        emit(ps, MsgIds.SC_BOX_EQUIP_INFO, msg.toByteArray());
    }

    public void sendWalletAll(PlayerSession ps, WalletDTOs.BalancesResp info) {
        if (ps == null || ps.getOutbound() == null) return;
        if (info == null || info.getBalances() == null || info.getBalances().isEmpty()) return;

        var all = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
        for (var e : info.getBalances().entrySet()) {
            Long itemId = e.getKey();
            Long amount = e.getValue();
            if (itemId == null) continue;

            all.addItemList(
                    Msgknapsack.PB_ItemData.newBuilder()
                            .setItemId(itemId.intValue())
                            .setNum(amount == null ? 0L : Math.max(0L, amount))
                            .build()
            );
        }

        emit(ps, MsgIds.SC_KNAPSACK_ALL_INFO, all.build().toByteArray());
    }

    // ====== Heartbeat ======
    public void sendHeartbeat(PlayerSession ps) {
        var resp = Msgserver.PB_SCHeartbeatResp.newBuilder()
                .setReserve(0)
                .build();
        emit(ps, MsgIds.SC_HEARTBEAT_RESP, resp.toByteArray());
    }

    // ====== Time ======
    public void sendTimeAck(PlayerSession ps, int serverTime, int openDays) {
        var t = Msgserver.PB_SCTimeAck.newBuilder()
                .setServerTime(serverTime)
                .setOpenDays(openDays)
                .build();
        emit(ps, MsgIds.SC_TIME_ACK, t.toByteArray());
    }

    // ====== RoleInfo ======
    public void sendRoleInfoAck(PlayerSession ps, RoleDTOs.RoleResp r) {
        if (r == null) return;

        var roleinfo = Msgrole.PB_RoleInfo.newBuilder()
                .setRoleId(nvlStrToInt(r.getRoleId()))
                .setName(bytesSafe(r.getName()))
                .setLevel(nvl(r.getLevel(), 1))
                .setCap(nvl(r.getCap(), 0L))
                .setHeadPicId(nvl(r.getHeadPicId(), 0))
                .setTitleId(nvl(r.getTitleId(), 0))
                .setGuildName(com.google.protobuf.ByteString.EMPTY)
                .setKnightLevel(0)
                .setHeadChar(com.google.protobuf.ByteString.EMPTY)
                .build();

        var ack = Msgrole.PB_SCRoleInfoAck.newBuilder()
                .setCurExp(nvl(r.getExp(), 0L))
                .setCreateTime(nvl(r.getCreateTimeEpochSec(), 0L))
                .setRoleinfo(roleinfo)
                //   .setAppearance(Msgrole.PB_Appearance.newBuilder().build())
                .build();

        // Bạn từng dùng 1400 cho SCRoleInfoAck → giữ nguyên để khớp client hiện tại.
        emit(ps, 1400, ack.toByteArray());
    }

    // ====== Knapsack (All + Single) ======
    public void sendKnapsackAll(PlayerSession ps, BagDTOs.BagView bag) {
        if (bag == null || bag.getSlots() == null) return;

        var b = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
        for (var s : bag.getSlots()) {
            if (s.getCount() <= 0) continue;
            b.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                    .setItemId(s.getItemId())
                    .setNum(s.getCount())
                    .build());
        }
        emit(ps, MsgIds.SC_KNAPSACK_ALL_INFO, b.build().toByteArray());
    }

    public void sendKnapsackSingle(PlayerSession ps, BagDTOs.BagView bag, int itemId) {
        long total = 0;
        if (bag != null && bag.getSlots() != null) {
            for (var s : bag.getSlots()) {
                if (s.getItemId() == itemId) total += s.getCount();
            }
        }
        var item = Msgknapsack.PB_ItemData.newBuilder()
                .setItemId(itemId)
                .setNum(total)
                .build();

        // Sửa lỗi cũ: phải build message bao ngoài PB_SCKnapsackSingleInfo rồi emit.
        var single = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                .setItem(item)
                .build();

        emit(ps, MsgIds.SC_KNAPSACK_SINGLE_INFO, single.toByteArray());
    }

    // ====== Equip list/bag ======
    public void sendEquipList(PlayerSession ps, EquipDTOs.ListResp list) {
        var b = Msgequip.PB_SCEquipListInfo.newBuilder();
        if (list != null && list.getItems() != null) {
            for (var e : list.getItems()) {
                var eb = Msgequip.PB_EquipData.newBuilder()
                        .setEquipType(e.getEquipType())
                        .setItemId(e.getItemId());
                if (e.getHp() != 0) eb.setHp(e.getHp());
                if (e.getAttack() != 0) eb.setAttack(e.getAttack());
                if (e.getDefend() != 0) eb.setDefend(e.getDefend());
                if (e.getSpeed() != 0) eb.setSpeed(e.getSpeed());
                if (e.getAttrType1() != 0) eb.setAttrType1(e.getAttrType1());
                if (e.getAttrValue1() != 0) eb.setAttrValue1(e.getAttrValue1());
                if (e.getAttrType2() != 0) eb.setAttrType2(e.getAttrType2());
                if (e.getAttrValue2() != 0) eb.setAttrValue2(e.getAttrValue2());
                b.addEquipList(eb);
            }
        }
        emit(ps, MsgIds.SC_EQUIP_LIST_INFO, b.build().toByteArray());
    }

    public static void sendEquipBagList(PlayerSession ps, BagDTOs.BagView bag, IntFunction<Integer> equipTypeLookup) {
        if (bag == null || bag.getSlots() == null || bag.getSlots().isEmpty()) {
            var empty = Msgequip.PB_SCEquipBagListInfo.newBuilder().build();
            emit(ps, MsgIds.SC_EQUIP_BAG_LIST_INFO, empty.toByteArray());
            return;
        }
        var b = Msgequip.PB_SCEquipBagListInfo.newBuilder();
        for (var s : bag.getSlots()) {
            int itemId = s.getItemId();
            int et = safeEquipType(equipTypeLookup, itemId);

            var ed = Msgequip.PB_EquipData.newBuilder()
                    .setItemId(itemId)
                    .setEquipType(et)
                    .build();

            var bd = Msgequip.PB_EquipBagData.newBuilder()
                    .setIndex(s.getSlotIndex())
                    .setBagData(ed)
                    .build();

            b.addBagList(bd);
        }
        emit(ps, MsgIds.SC_EQUIP_BAG_LIST_INFO, b.build().toByteArray());
    }

    // GIỮ để tương thích cũ (fallback = 1)
    public static void sendEquipBagList(PlayerSession ps, BagDTOs.BagView bag) {
        sendEquipBagList(ps, bag, id -> 1);
    }

    private static int safeEquipType(IntFunction<Integer> f, int itemId) {
        try {
            Integer v = f != null ? f.apply(itemId) : null;
            if (v == null || v <= 0) return 1; // fallback hợp lý hơn 0
            return v;
        } catch (Throwable ignore) {
            return 1;
        }
    }

    // ====== Box ======
    public void sendBoxInfo(PlayerSession ps, BoxDTOs.InfoResp info) {
        if (info == null) return;

        var sc = Msgbox.PB_SCBoxInfo.newBuilder()
                .setBoxLevel(Math.max(0, info.getBoxLevel()))
                .setBuyTimes(Math.max(0, info.getBoxBuyTimes()))
                .setTimestamp((int) Math.max(0, info.getLevelUpEndEpoch()))
                .setArenaItemNum(Math.max(0, info.getArenaItemNum() == 0 ? 0 : info.getArenaItemNum()))
                .setShiZhuangNum(Math.max(0, info.getShiZhuangNum() == 0 ? 0 : info.getShiZhuangNum()))
                .setLevelFetchFlag(Math.max(0, info.getLevelFetchFlag()))
                .build();

        emit(ps, MsgIds.SC_BOX_INFO, sc.toByteArray());

        // Nếu bạn có SC cho "pending equip", có thể map & emit ở đây.
        // Không giả định proto → bỏ qua.

        // Nếu có equip vừa mở (pending) -> bắn 1615
        var pending = info.getPending();
        if (pending != null && !pending.isEmpty()) {
            var equipDataOpt = buildEquipDataFromPending(pending);
            var scEquip = Msgbox.PB_SCBoxEquipInfo.newBuilder()
                    .setIsNew(1); // "是(没更换过):1"

            equipDataOpt.ifPresent(scEquip::setEquipInfo);
            emit(ps, MsgIds.SC_BOX_EQUIP_INFO, scEquip.build().toByteArray());
        }
    }

    /**
     * Cố gắng map Map<String,Object> 'pending' -> PB_EquipData.
     * Nếu không đủ dữ liệu quan trọng thì trả Optional.empty()
     */
    private Optional<Msgequip.PB_EquipData> buildEquipDataFromPending(Map<String, Object> p) {
        Integer itemId = pickInt(p, "itemId", "item_id", "id");
        if (itemId == null || itemId <= 0) {
            return Optional.empty();
        }

        var b = Msgequip.PB_EquipData.newBuilder();
        // Các field chính
        setIfPresent(b::setEquipType, p, "equipType", "equip_type", "type");
        b.setItemId(itemId);

        // Stats cơ bản
        setIfPresent(b::setHp, p, "hp");
        setIfPresent(b::setAttack, p, "attack", "atk");
        setIfPresent(b::setDefend, p, "defend", "def");
        setIfPresent(b::setSpeed, p, "speed", "spd");

        // Attrs phụ
        setIfPresent(b::setAttrType1, p, "attrType1", "attr_type1");
        setIfPresent(b::setAttrValue1, p, "attrValue1", "attr_value1");
        setIfPresent(b::setAttrType2, p, "attrType2", "attr_type2");
        setIfPresent(b::setAttrValue2, p, "attrValue2", "attr_value2");

        return Optional.of(b.build());
    }

    private Integer pickInt(Map<String, Object> m, String... keys) {
        for (var k : keys) {
            var v = m.get(k);
            if (v == null) continue;
            if (v instanceof Number n) return n.intValue();
            try {
                var s = v.toString().trim();
                if (!s.isEmpty()) return Integer.parseInt(s);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private void setIfPresent(java.util.function.IntConsumer setter,
                              Map<String, Object> m,
                              String... keys) {
        Integer v = pickInt(m, keys);
        if (v != null) setter.accept(v);
    }


    /**
     * Gửi SC_BOX_SETING_INFO (PB_SCBoxSetingInfo).
     * Map từ BoxSettingResp -> PB_BoxSet theo đúng tên field client cần.
     */
    public static void sendBoxSettingInfo(PlayerSession ps, BoxDTOs.BoxSettingResp set) {
        if (ps == null || ps.getOutbound() == null || set == null) return;

        // helper: null-safe -> int
        Function<Number, Integer> nn = n -> (n == null ? 0 : n.intValue());

        // PB_BoxSet (chú ý: equipEqality là tên theo proto client, không phải "quality")
        var boxSet = Msgbox.PB_BoxSet.newBuilder()
                .setEquipEqality(nn.apply(set.getEquipEqality()))
                .setConditionFirstMark(nn.apply(set.getConditionFirstMark()))
                .setConditionFirst1(nn.apply(set.getConditionFirst1()))
                .setConditionFirst2(nn.apply(set.getConditionFirst2()))
                .setConditionSecondMark(nn.apply(set.getConditionSecondMark()))
                .setConditionSecond1(nn.apply(set.getConditionSecond1()))
                .setConditionSecond2(nn.apply(set.getConditionSecond2()))
                .setRetainMark(nn.apply(set.getRetainMark()))
                .setChallengeMark(nn.apply(set.getChallengeMark()))
                .setEquipCapMark(nn.apply(set.getEquipCapMark()))
                .setEquipSellMark(nn.apply(set.getEquipSellMark()))
                .setOpenFiveMark(nn.apply(set.getOpenFiveMark()))
                .build();

        var resp = Msgbox.PB_SCBoxSetingInfo.newBuilder()
                .setBoxSet(boxSet)
                .build();

        emit(ps, MsgIds.SC_BOX_SETING_INFO, resp.toByteArray());
    }

    // ====== Fumo (chưa có proto → chỉ log, không emit) ======
    public void sendEquipFumoList(PlayerSession ps, EquipFumoDTOs.FumoListResp resp) {
        log.debug("[Emitters] FumoListResp received but no FuMo proto defined → skip emit");
    }

    public void sendEquipFumoOne(PlayerSession ps, EquipFumoDTOs.FumoOneResp resp) {
        log.debug("[Emitters] FumoOneResp received but no FuMo proto defined → skip emit");
    }

    // ====== commons ======
    private com.google.protobuf.ByteString bytesSafe(String s) {
        if (org.apache.commons.lang3.StringUtils.isBlank(s)) {
            return com.google.protobuf.ByteString.EMPTY;
        }
        return com.google.protobuf.ByteString.copyFrom(
                s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int nvl(Integer v, int d) {
        return v == null ? d : v;
    }

    private long nvl(Long v, long d) {
        return v == null ? d : v;
    }

    private int nvlStrToInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }


    // NEW: gửi toàn bộ equip đang mặc về client

    /**
     * Gọi tiện: truyền thẳng Feign ResultDTO hoặc ListResp đều được.
     */
    public void sendEquipAll(PlayerSession ps, Object maybeWrapper) {
        if (maybeWrapper == null) return;

        // Nếu đã là ListResp
        if (maybeWrapper instanceof EquipDTOs.ListResp lr) {
            sendEquipAll(ps, lr);
            return;
        }

        // Thử unwrap kiểu ResultDTO<T> bằng reflection: getData() -> ListResp
        try {
            var m = maybeWrapper.getClass().getMethod("getData");
            Object data = m.invoke(maybeWrapper);
            if (data instanceof EquipDTOs.ListResp lr) {
                sendEquipAll(ps, lr);
                return;
            }
        } catch (Exception ignore) {
        }

        log.warn("sendEquipAll: unsupported payload type {}", maybeWrapper.getClass().getName());
    }

    /**
     * Gửi PB_SCEquipListInfo (msgId=1605) chứa danh sách PB_EquipData.
     */
    public void sendEquipAll(PlayerSession ps, EquipDTOs.ListResp all) {
        if (ps == null || ps.getOutbound() == null || all == null) return;

        var listBuilder = Msgequip.PB_SCEquipListInfo.newBuilder();
        for (var it : safeItems(all)) {
            var ed = Msgequip.PB_EquipData.newBuilder()
                    .setEquipType(nz(it.getEquipType()))
                    .setItemId(nz(it.getItemId()))
                    .build();

            // Bổ sung chỉ số nếu >0 để gọn payload
            var edB = ed.toBuilder();
            if (nz(it.getHp()) > 0) edB.setHp(nz(it.getHp()));
            if (nz(it.getAttack()) > 0) edB.setAttack(nz(it.getAttack()));
            if (nz(it.getDefend()) > 0) edB.setDefend(nz(it.getDefend()));
            if (nz(it.getSpeed()) > 0) edB.setSpeed(nz(it.getSpeed()));
            if (nz(it.getAttrType1()) > 0) edB.setAttrType1(nz(it.getAttrType1()));
            if (nz(it.getAttrValue1()) > 0) edB.setAttrValue1(nz(it.getAttrValue1()));
            if (nz(it.getAttrType2()) > 0) edB.setAttrType2(nz(it.getAttrType2()));
            if (nz(it.getAttrValue2()) > 0) edB.setAttrValue2(nz(it.getAttrValue2()));

            // Thêm vào listBuilder: thử nhiều tên field phổ biến để khỏi lệ thuộc đúng tên trong .proto
            if (!addEquipDataReflect(listBuilder, edB.build())) {
                // Nếu không khớp field nào -> log để bạn đổi tên cho đúng
                log.warn("PB_SCEquipListInfo builder: cannot find add*(PB_EquipData) adder; please align with .proto field name");
            }
        }

        // 1605 ↔ PB_SCEquipListInfo
        emit(ps, MsgIds.SC_EQUIP_LIST_INFO, listBuilder.build().toByteArray());
    }

    /**
     * Thêm phần tử vào builder bằng reflection, thử các tên add phổ biến.
     */
    private boolean addEquipDataReflect(Object builder, Msgequip.PB_EquipData val) {
        // Thử các tên thường gặp: equip_list / equip / data / equipData / list / items...
        String[] tryNames = {
                "addEquipList", "addEquip", "addData", "addEquipData", "addList", "addItems", "addItem"
        };
        for (String name : tryNames) {
            try {
                var m = builder.getClass().getMethod(name, Msgequip.PB_EquipData.class);
                m.invoke(builder, val);
                return true;
            } catch (Exception ignore) {
            }
        }
        // Quét mọi method add*(PB_EquipData)
        for (var m : builder.getClass().getMethods()) {
            if (!m.getName().startsWith("add") || m.getParameterCount() != 1) continue;
            var pt = m.getParameterTypes()[0];
            if (pt.isAssignableFrom(Msgequip.PB_EquipData.class)) {
                try {
                    m.invoke(builder, val);
                    return true;
                } catch (Exception ignore) {
                }
            }
        }
        return false;
    }


    // ====== helpers nhỏ cho null-safety ======
    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static int nz(int v) {
        return v;
    }

    private static List<EquipDTOs.EquipItem> safeItems(EquipDTOs.ListResp all) {
        try {
            var m = EquipDTOs.ListResp.class.getMethod("getItems");
            @SuppressWarnings("unchecked")
            List<EquipDTOs.EquipItem> items = (List<EquipDTOs.EquipItem>) m.invoke(all);
            return items != null ? items : List.of();
        } catch (Exception ignore) {
            // nếu ListResp là record(items) thay vì Lombok @Getter
            try {
                var m2 = EquipDTOs.ListResp.class.getMethod("items");
                @SuppressWarnings("unchecked")
                List<EquipDTOs.EquipItem> items = (List<EquipDTOs.EquipItem>) m2.invoke(all);
                return items != null ? items : List.of();
            } catch (Exception e2) {
                return List.of();
            }
        }
    }

    // ====== Shop ======
    public void sendShopInfo(PlayerSession ps, ShopDTOs.InfoResp info) {
        if (ps == null || ps.getOutbound() == null || info == null) return;

        var out = Msgother.PB_SCShopInfo.newBuilder();
        boolean added = false;

        // ---- Try 1: InfoResp có Map<?,?> (vd: buys / index2BuyNum / map / data)
        Map<?, ?> map = tryGetMap(info, "buys", "index2BuyNum", "map", "data");
        if (map != null && !map.isEmpty()) {
            for (var e : map.entrySet()) {
                int index = toInt(e.getKey());
                int buyNum = Math.max(0, toInt(e.getValue()));
                if (index > 0) {
                    out.addDataList(Msgother.PB_ShopData.newBuilder()
                            .setIndex(index)
                            .setBuyNum(buyNum)
                            .build());
                    added = true;
                }
            }
        }

        // ---- Try 2: InfoResp có List<?> (vd: dataList / items / list / records)
        if (!added) {
            List<?> list = tryGetList(info, "dataList", "items", "list", "records");
            if (list != null && !list.isEmpty()) {
                for (Object row : list) {
                    int index = reflectInt(row, "index", "id", "seq", "idOrIndex");
                    int buyNum = reflectInt(row, "buyNum", "count", "num", "times");
                    if (index > 0) {
                        out.addDataList(Msgother.PB_ShopData.newBuilder()
                                .setIndex(index)
                                .setBuyNum(Math.max(0, buyNum))
                                .build());
                        added = true;
                    }
                }
            }
        }

        if (!added) {
            // Không map được gì -> vẫn emit gói rỗng để client tự xử lý mặc định
            log.warn("sendShopInfo: cannot map InfoResp={}, emitting empty PB_SCShopInfo", info.getClass().getName());
        }

        emit(ps, MsgIds.SC_SHOP_INFO, out.build().toByteArray());
    }

    /* ===== Helpers (private) ===== */

    @SuppressWarnings("unchecked")
    private Map<?, ?> tryGetMap(Object bean, String... names) {
        for (String n : names) {
            Object val = invokeNoArg(bean, n, "get" + capitalize(n));
            if (val instanceof Map<?, ?> m) return m;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<?> tryGetList(Object bean, String... names) {
        for (String n : names) {
            Object val = invokeNoArg(bean, n, "get" + capitalize(n));
            if (val instanceof List<?> l) return l;
        }
        return null;
    }

    private int reflectInt(Object bean, String... fieldNames) {
        for (String n : fieldNames) {
            Object v = invokeNoArg(bean, n, "get" + capitalize(n));
            int x = toInt(v);
            if (x != 0) return x;
        }
        return 0;
    }

    private Object invokeNoArg(Object bean, String... candidates) {
        for (String name : candidates) {
            try {
                Method m = bean.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(bean);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                log.debug("invokeNoArg {} failed: {}", name, ex.toString());
            }
        }
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public static void sendDisconnectNotice(PlayerSession ps, int reason) {
        if (ps == null) return;

        Msgserver.PB_SCDisconnectNotice.Builder b = Msgserver.PB_SCDisconnectNotice.newBuilder()
                .setReason(reason);

        // role_id là int32 trong proto; roleId của mình là String => parse an toàn
        int rid = nvlStrToInt(ps.getRoleId());
        if (rid > 0) {
            b.setRoleId(rid);
        }

        // proto có user_name (string) — nếu có username thì set
        if (io.micrometer.common.util.StringUtils.isNotEmpty(ps.getUsername())) {
            b.setUserName(ps.getUsername());
        }

        // MsgId 9001: SC_DISCONNECT_NOTICE (bạn đã RegisterMsg ở init)
        emit(ps, MsgIds.SC_DISCONNECT_NOTICE, b.build().toByteArray());
    }

    private static int nvl(Integer x) { return x == null ? 0 : x; }
}