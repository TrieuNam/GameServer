package com.southMillion.webSocket_server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.net.PacketCodec;
import com.southMillion.webSocket_server.service.SessionRegistry;
import com.southMillion.webSocket_server.service.client.*;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.dto.gift.GiftDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Game WebSocket entrypoint.
 * - Login/Bootstrap
 * - Knapsack (USE item có tích hợp gift-service, fallback consume túi)
 * - Equip (+Fumo)
 * - Box
 * - Shop
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameWsHandler implements WebSocketHandler {

    private final SessionRegistry registry;
    private final SessionHttpClient sessionFeign;

    // Feign
    private final RoleHttpClient roleFeign;
    private final BagPublicHttpClient bagPublicFeign;
    private final BagInternalFeign bagInternalFeign;
    private final EquipHttpClient equipFeign;
    private final EquipFumoFeign equipFumoFeign;
    private final ConfigFeign configFeign;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final byte BAG_COMMON = 0;          // túi thường
    private static final String REASON_FIRST_LOGIN_BOX = "FIRST_LOGIN_BOX";
    private static final int SRC_MSG_LOGIN = 7056;     // tuỳ bạn quy ước
    private static final int SRC_OP_GRANT = 1;
    private static final long DEFAULT_BOX_ITEM_ID = 40004L;


    private final ShopFeign shopFeign;
    private final WalletHttpClient walletFeign; // ĐÃ dùng trực tiếp trong handler này
    private final BoxFeign boxFeign;
    private final GiftFeign giftFeign;

    private static final byte BAG_EQUIP = 1; // nếu dùng túi riêng chứa equip

    /**
     * Danh sách itemId của tiền tệ/virtual muốn hiển thị cho client (map sang KnapsackSingleInfo).
     */
    private static final List<Long> DEFAULT_WALLET_IDS = List.of(40000L, 40001L, 40087L);
    // 40000=Gold, 40001=Diamond, 40087=Silver (điều chỉnh theo dự án)

    // req_type cho Equip/Fumo (điền trùng client)
    private static final class EquipOp {
        static final int WEAR = 1;
        // Fumo block:
        static final int FUMO_LIST = 11;
        static final int FUMO_ONE = 12;
        static final int FUMO_ADD_EXP = 13;
        static final int FUMO_ACTIVATE = 14;
        static final int FUMO_RESET = 15;
    }
    private record StarterBoxCfg(long itemId, int count) {}

    private Mono<StarterBoxCfg> loadStarterBoxCfg(String token) {
        return FeignCall.withToken(token, "cfg.roleexp", () -> configFeign.roleExpRaw())
                .map(resp -> resp != null ? resp.getBody() : null)
                .map(bytes -> {
                    if (bytes == null || bytes.length == 0) {
                        throw new IllegalStateException("roleexp.json empty");
                    }
                    try {
                        JsonNode root = JSON.readTree(bytes);

                        // other có thể là object hoặc array[0]; key có thể "other"/"Other"/"OtherCfg"
                        JsonNode other = root.get("other");
                        if (other == null) other = root.get("Other");
                        if (other == null) other = root.get("OtherCfg");
                        JsonNode node = (other != null && other.isArray() && other.size() > 0) ? other.get(0) : other;

                        long itemId = DEFAULT_BOX_ITEM_ID;
                        int count = 0;

                        if (node != null) {
                            JsonNode boxId = node.has("box_id") ? node.get("box_id") : node.get("boxId");
                            if (boxId != null && !boxId.isNull()) {
                                itemId = boxId.isNumber() ? boxId.asLong() : Long.parseLong(boxId.asText());
                            }
                            JsonNode boxNum = node.has("box_num") ? node.get("box_num") : node.get("boxNum");
                            if (boxNum != null && !boxNum.isNull()) {
                                count = boxNum.isNumber() ? boxNum.asInt() : Integer.parseInt(boxNum.asText());
                            }
                        }
                        return new StarterBoxCfg(itemId, Math.max(0, count));
                    } catch (Exception e) {
                        throw new IllegalStateException("Parse roleexp.json failed: " + e.getMessage(), e);
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("loadStarterBoxCfg fail: {}", ex.toString());
                    // Fallback: giữ itemId default, count=0 để không cấp nhầm.
                    return Mono.just(new StarterBoxCfg(DEFAULT_BOX_ITEM_ID, 0));
                });
    }

    private Mono<Void> pushBoxInfoNow(String token, PlayerSession ps) {
        if (!org.springframework.util.StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        return FeignCall.withToken(token, "box.info", () -> boxFeign.info(ps.getRoleId()))
                .doOnNext(info -> sendBoxInfo(ps, info))
                .onErrorResume(ex -> {
                    log.info("box.info optional failed: {}", ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> grantStarterBox(String token, String roleId, StarterBoxCfg cfg) {
        if (cfg == null || cfg.count() <= 0) return Mono.empty();

        // BagDTOs.ItemDelta(int itemId, long count, boolean bound, String reason)
        var itemDelta = new BagDTOs.ItemDelta(
                (int) cfg.itemId(),    // 40004
                cfg.count(),           // 60
                false,
                REASON_FIRST_LOGIN_BOX,
                null,
                true                   // << yêu cầu singleStack
        );
        var req = new BagDTOs.AddItemReq(roleId, BAG_COMMON, List.of(itemDelta), SRC_MSG_LOGIN, SRC_OP_GRANT);

        return FeignCall.withToken(token, "bag.add", () -> bagInternalFeign.add(req))
                .doOnNext(resp -> log.info("Granted starter box roleId={} itemId={} x{} added={}, overflow={}",
                        roleId, cfg.itemId(), cfg.count(),
                        (resp != null ? resp.added() : null),
                        (resp != null ? resp.overflow() : null)))
                .onErrorResume(ex -> {
                    log.warn("grantStarterBox failed roleId={}, err={}", roleId, ex.toString());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var ps = PlayerSession.builder()
                .ws(session)
                .outbound(Sinks.many().unicast().onBackpressureBuffer()) // or .buffer() dung lượng mặc định
                .build();

        // SEND: chỉ subscribe 1 lần
        Mono<Void> send = session.send(
                ps.getOutbound().asFlux()
                        .map(bytes -> session.binaryMessage(buf -> buf.wrap(bytes)))
        ).doFinally(sig -> {
            // đóng sink khi ws kết thúc
            ps.getOutbound().tryEmitComplete();
        });


        // RECV: như bạn đã làm (đừng block trong pipeline)
        Mono<Void> recv = session.receive()
                .map(WebSocketMessage::getPayload)
                .map(buf -> {
                    ByteBuffer bb = buf.asByteBuffer();
                    byte[] arr = new byte[bb.remaining()];
                    bb.get(arr);
                    return arr;
                })
                .concatMap(raw -> {
                    try {
                        var pkt = PacketCodec.decode(raw);
                        return route(ps, pkt.msgId(), pkt.payload()); // phải trả về Mono<Void> không block
                    } catch (Throwable t) {
                        log.warn("decode error: {}", t.getMessage());
                        return Mono.empty();
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("WS recv error: {}", ex.getMessage());
                    return Mono.empty();
                })
                .then();

        // ✨ Chạy song song send & recv, không block
        return Mono.when(recv, send).then();
    }

    // ==== Router theo msgId
    private Mono<Void> route(PlayerSession ps, int msgId, byte[] payload) {
        if (log.isDebugEnabled()) {
            log.debug("route: roleId={}, msgId={}", ps.getRoleId(), msgId);
        }

        return switch (msgId) {
            case MsgIds.CS_HEARTBEAT_REQ -> onHeartbeat(ps, payload);
            case MsgIds.CS_TIME_REQ -> onTimeReq(ps, payload);
            case MsgIds.CS_LOGIN_REQ -> onLogin(ps, payload);

            case MsgIds.CS_KNAPSACK_REQ -> onKnapsack(ps, payload);
            case MsgIds.CS_EQUIP_REQ -> onEquip(ps, payload);

            case MsgIds.CS_BOX_REQ -> onBox(ps, payload);
            case MsgIds.CS_SHOP_BUY_REQ -> onShop(ps, payload);

            default -> {
                log.debug("Unhandled msgId={}", msgId);
                yield Mono.empty();
            }
        };
    }

    // =============================================================================================
    //  BOX HANDLER
    // =============================================================================================

    private Mono<Void> onBox(PlayerSession ps, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        try {
            var req = Msgbox.PB_CSBoxReq.parseFrom(payload);
            int type = req.hasReqType() ? req.getReqType() : -1;
            int param = req.hasParam() ? req.getParam() : 0;

            switch (type) {
                case 1 -> handleBoxOpen(ps, param);       // OPEN_BOX: param 0=1 lần; 1=5 lần
                case 2 -> handleBoxWear(ps);              // WEAR_EQUIP
                case 3 -> handleBoxSell(ps);              // SELL
                case 4 -> handleBoxLevelBuy(ps);          // LEVEL_BUY
                case 5 -> handleBoxLevelUp(ps);           // LEVEL_UP
                case 6 -> handleBoxSpeedUp(ps, param);    // SPEED_UP
                default -> log.debug("onBox: ignore type={}", type);
            }
        } catch (Exception ex) {
            log.warn("onBox error roleId={}, ex={}", ps.getRoleId(), ex.toString());
        }
        return Mono.empty();
    }

    // ============ handlers: tất cả đều reactive, KHÔNG gọi Feign trực tiếp ============

    private Mono<Void> handleBoxOpen(PlayerSession ps, int param) {
        final String token = ps.getSessionId();
        final int count = (param == 1) ? 5 : 1;
        final int roleLevel = 1; // TODO: lấy thật từ role-service nếu cần

        // Dùng defer để mọi thứ chỉ chạy khi subscribe
        return Mono.defer(() -> {
            var openReq = new BoxDTOs.OpenReq();
            openReq.setRoleId(ps.getRoleId());
            openReq.setCount(count);
            openReq.setRoleLevel(roleLevel);

            // 1) Gọi open (defer để không thực thi sớm)
            Mono<Void> openMono = Mono.defer(() ->
                    FeignCall.withToken(token, "box.open", () -> boxFeign.open(openReq)).then()
            );

            // 2) Sau khi open xong, chạy các refresh song song (đều defer)
            Mono<Void> infoMono = Mono.defer(() ->
                    FeignCall.withToken(token, "box.info", () -> boxFeign.info(ps.getRoleId()))
                            .doOnNext(info -> sendBoxInfo(ps, info))
                            .onErrorResume(ex -> {
                                log.info("box.info optional: {}", ex.getMessage());
                                return Mono.empty();
                            })
                            .then()
            );

            Mono<Void> equipInfoMono = Mono.defer(() ->
                    Mono.fromRunnable(() -> sendBoxEquipInfo(ps, /*isNew*/ true))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then()
            );

            Mono<Void> bagMono = Mono.defer(() -> safeRefreshKnapsack(ps)); // đã là Mono, bọc thêm defer cho chắc
            Mono<Void> walletMono = Mono.defer(() ->
                    Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then()
            );

            // 3) Chuỗi: open xong -> chạy when(...) song song
            return openMono.then(Mono.when(infoMono, equipInfoMono, bagMono, walletMono)).then();
        }).onErrorResume(ex -> {
            log.warn("handleBoxOpen error roleId={}, ex={}", ps.getRoleId(), ex.toString());
            return Mono.empty();
        });
    }

    private Mono<Void> handleBoxWear(PlayerSession ps) {
        String token = ps.getSessionId();

        var wearReq = new BoxDTOs.WearReq();
        wearReq.setRoleId(ps.getRoleId());

        Mono<Void> wearMono =
                FeignCall.withToken(token, "box.wear", () -> boxFeign.wear(wearReq)).then();

        Mono<Void> equipListMono = safeRefreshEquipList(ps);       // viết kiểu FeignCall ở helper này
        Mono<Void> pingEquipMono = Mono.fromRunnable(() -> sendBoxEquipInfo(ps, /*isNew*/ false))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        return wearMono.then(Mono.when(equipListMono, pingEquipMono, walletMono)).then();
    }

    private Mono<Void> handleBoxSell(PlayerSession ps) {
        String token = ps.getSessionId();

        var sellReq = new BoxDTOs.SellReq();
        sellReq.setRoleId(ps.getRoleId());

        Mono<Void> sellMono =
                FeignCall.withToken(token, "box.sell", () -> boxFeign.sell(sellReq)).then();

        Mono<Void> notifyMono = Mono.fromRunnable(() -> {
                    var msg = Msgbox.PB_SCBoxSellInfo.newBuilder()
                            .setSellCoin(0).setSellExp(0).build();
                    emit(ps, MsgIds.SC_BOX_SELL_INFO, msg.toByteArray());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        Mono<Void> bagMono = safeRefreshKnapsack(ps);
        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        return sellMono.then(Mono.when(notifyMono, bagMono, walletMono)).then();
    }

    private Mono<Void> handleBoxLevelBuy(PlayerSession ps) {
        String token = ps.getSessionId();
        var req0 = new BoxDTOs.SimpleReq();
        req0.setRoleId(ps.getRoleId());

        Mono<Void> buyMono =
                FeignCall.withToken(token, "box.buy", () -> boxFeign.buy(req0)).then();

        Mono<Void> infoMono =
                FeignCall.withToken(token, "box.info", () -> boxFeign.info(ps.getRoleId()))
                        .doOnNext(info -> sendBoxInfo(ps, info))
                        .onErrorResume(ex -> { log.info("box.info optional: {}", ex.getMessage()); return Mono.empty(); })
                        .then();

        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        return buyMono.then(Mono.when(infoMono, walletMono)).then();
    }

    private Mono<Void> handleBoxLevelUp(PlayerSession ps) {
        String token = ps.getSessionId();
        var req0 = new BoxDTOs.SimpleReq();
        req0.setRoleId(ps.getRoleId());

        Mono<Void> upMono =
                FeignCall.withToken(token, "box.levelUp", () -> boxFeign.levelUp(req0)).then();

        Mono<Void> infoMono =
                FeignCall.withToken(token, "box.info", () -> boxFeign.info(ps.getRoleId()))
                        .doOnNext(info -> sendBoxInfo(ps, info))
                        .onErrorResume(ex -> { log.info("box.info optional: {}", ex.getMessage()); return Mono.empty(); })
                        .then();

        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        return upMono.then(Mono.when(infoMono, walletMono)).then();
    }

    private Mono<Void> handleBoxSpeedUp(PlayerSession ps, int param) {
        String token = ps.getSessionId();
        var req0 = new BoxDTOs.QuickenReq();
        req0.setRoleId(ps.getRoleId());
        req0.setNum(Math.max(1, param));

        Mono<Void> fastMono =
                FeignCall.withToken(token, "box.quicken", () -> boxFeign.quicken(req0)).then();

        Mono<Void> infoMono =
                FeignCall.withToken(token, "box.info", () -> boxFeign.info(ps.getRoleId()))
                        .doOnNext(info -> sendBoxInfo(ps, info))
                        .onErrorResume(ex -> { log.info("box.info optional: {}", ex.getMessage()); return Mono.empty(); })
                        .then();

        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        return fastMono.then(Mono.when(infoMono, walletMono)).then();
    }

    private void sendBoxInfo(PlayerSession ps, BoxDTOs.InfoResp info) {
        if (info == null) return;

        // Map chuẩn theo PB_SCBoxInfo (MsgId:1616)
        var scInfo = Msgbox.PB_SCBoxInfo.newBuilder()
                .setBoxLevel(Math.max(0, info.getBoxLevel()))
                .setBuyTimes(Math.max(0, info.getBoxBuyTimes()))
                .setTimestamp((int) Math.max(0, info.getLevelUpEndEpoch()))
                .setArenaItemNum(0) // hiện chưa có trong DTO
                .setShiZhuangNum(0) // hiện chưa có trong DTO
                .setLevelFetchFlag(Math.max(0, info.getLevelFetchFlag()))
                .build();

        emit(ps, MsgIds.SC_BOX_INFO, scInfo.toByteArray());

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

    private void sendBoxEquipInfo(PlayerSession ps, boolean isNew) {
        var msg = Msgbox.PB_SCBoxEquipInfo.newBuilder()
                .setIsNew(isNew ? 1 : 0)
                .build();
        emit(ps, MsgIds.SC_BOX_EQUIP_INFO, msg.toByteArray());
    }

    // =============================================================================================
    //  SHOP HANDLER
    // =============================================================================================

    private Mono<Void> onShop(PlayerSession ps, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        try {
            var req = Msgother.PB_CSShopBuyReq.parseFrom(payload);
            int index = req.hasIndex() ? req.getIndex() : 0;
            int num = req.hasNum() ? req.getNum() : 1;

            // Map sang shop-service: giả định COMMON shop
            var buyReq = new ShopDTOs.BuyReq(
                    ps.getRoleId(),
                    ShopDTOs.Kind.COMMON,
                    index,
                    Math.max(1, num),
                    BAG_COMMON, // nhận item vào bag thường
                    BAG_COMMON  // dùng ví trong bag thường (nếu ví là item ảo)
            );
            var res = shopFeign.buy(buyReq);

            // Thành công -> refresh knapsack + ví + bắn shopInfo (v1 để trống)
            if (res != null && Boolean.TRUE.equals(res.ok()) && res.data() != null && res.data().ok()) {
                safeRefreshKnapsack(ps);
                refreshWalletAndEmit(ps);

                var info = Msgother.PB_SCShopInfo.newBuilder().build();
                emit(ps, MsgIds.SC_SHOP_INFO, info.toByteArray());
            } else {
                // thất bại: vẫn refresh bag + ví cho chắc (trong trường hợp upstream rollback)
                safeRefreshKnapsack(ps);
                refreshWalletAndEmit(ps);
            }
        } catch (Exception ex) {
            log.warn("onShop error roleId={}, ex={}", ps.getRoleId(), ex.toString());
        }
        return Mono.empty();
    }

    // =============================================================================================
    //  Heartbeat / Time
    // =============================================================================================

    private Mono<Void> onHeartbeat(PlayerSession ps, byte[] payload) {
        try {
            var req = Msgserver.PB_CSHeartbeatReq.parseFrom(payload);
            var resp = Msgserver.PB_SCHeartbeatResp.newBuilder()
                    .setReserve(req.hasReserve() ? req.getReserve() : 0)
                    .build();
            ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_HEARTBEAT_RESP, resp.toByteArray()));
        } catch (InvalidProtocolBufferException e) {
            log.warn("HB parse error: {}", e.getMessage());
        }
        return Mono.empty();
    }

    private Mono<Void> onTimeReq(PlayerSession ps, byte[] _payload) {
        long now = Instant.now().getEpochSecond();
        var ack = Msgserver.PB_SCTimeAck.newBuilder()
                .setServerTime((int) now)
                .setServerRealStartTime(0)
                .build();
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_TIME_ACK, ack.toByteArray()));
        return Mono.empty();
    }

    // =============================================================================================
    //  Login + bootstrap
    // =============================================================================================

    private Mono<Void> onLogin(PlayerSession ps, byte[] payload) {
        final Msglogin.PB_CSLoginToAccount req;
        try {
            req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            log.warn("7056 parse error: {}", e.getMessage());
            sendLoginAck(ps, /*result*/ 4, /*forbid*/ 0);
            return Mono.empty();
        }

        String token = req.hasLoginStr() ? req.getLoginStr() : null;
        if (!StringUtils.hasText(token)) {
            log.warn("7056 without login_str");
            sendLoginAck(ps, 2, 0);
            return Mono.empty();
        }
        AtomicBoolean createdNow = new AtomicBoolean(false);
        // 1) introspect — nếu service yêu cầu header Authorization, interceptor sẽ tự add
        return FeignCall.withToken(token, () -> sessionFeign.introspect(token))
                .flatMap(r -> {
                    if (r == null || !r.isActive()) {
                        sendLoginAck(ps, 1, 0);
                        return Mono.empty();
                    }

                    ps.setUserId(r.getUserId());
                    ps.setUsername(r.getUsername());
                    ps.setSessionId(r.getSessionId());
                    ps.setLoggedIn(true);

                    // 2) Lấy danh sách role
                    return FeignCall.withToken(token, () -> roleFeign.list(ps.getUserId()));
                })
                .flatMap(roles -> {
                    String picked = null;
                    if (roles != null && roles.getItems() != null && !roles.getItems().isEmpty()) {
                        picked = roles.getItems().stream()
                                .min(Comparator.comparing(RoleDTOs.RoleResp::getRoleId))
                                .map(RoleDTOs.RoleResp::getRoleId)
                                .orElse(null);
                    }

                    if (org.springframework.util.StringUtils.hasText(picked)) {
                        ps.setRoleId(picked);
                        registry.updateBindings(ps);
                        return Mono.empty(); // KHÔNG bootstrap ở đây
                    }

                    // 3) Không có role -> tạo mới (gọi Feign thật sự)
                    return FeignCall.withToken(token, () -> roleFeign.create(buildDefaultCreateRoleReq(ps)))
                            .doOnNext(created -> {
                                if (created != null && org.springframework.util.StringUtils.hasText(created.getRoleId())) {
                                    ps.setRoleId(created.getRoleId());
                                    createdNow.set(true);
                                } else {
                                    log.warn("create role returns null/empty roleId for userId={}", ps.getUserId());
                                }
                                registry.updateBindings(ps);
                            })
                            .then();
                })
                // 4) Gửi ACK
                .then(sendLoginAck(ps, 0, 0))
                // Nếu vừa tạo role -> cấp box khởi tạo + (optional) đẩy box.info
                .then(Mono.defer(() -> {
                    if (!createdNow.get() || !org.springframework.util.StringUtils.hasText(ps.getRoleId())) return Mono.empty();
                    return loadStarterBoxCfg(token)
                            .flatMap(cfg -> grantStarterBox(token, ps.getRoleId(), cfg))
                            .then(pushBoxInfoNow(token, ps)); // optional
                }))
                // 5) Bootstrap SAU KHI roleId đã được set — dùng defer để kiểm tra lúc subscribe
                .then(Mono.defer(() ->
                        org.springframework.util.StringUtils.hasText(ps.getRoleId())
                                ? bootstrapAfterLogin(token, ps)
                                : Mono.empty()
                ))
                .onErrorResume(ex -> {
                    log.warn("login flow error: {}", ex.toString());
                    sendLoginAck(ps, 4, 0);
                    return Mono.empty();
                });
    }

    // Tạo tên role mặc định: username hoặc "Player_xxxx" với 4 ký tự cuối userId
    private static String defaultRoleName(String username, String userId) {
        if (StringUtils.hasText(username)) return username;
        String suffix = (userId != null && userId.length() >= 4)
                ? userId.substring(userId.length() - 4)
                : "0000";
        return "Player_" + suffix;
    }

    /**
     * Xây CreateRoleReq “an toàn”:
     * - new CreateRoleReq()
     * - setUserId(...)
     * - setNickname(...) / setRoleName(...) / setName(...) (tuỳ field thật sự tồn tại)
     * - có thể set mặc định class/job/gender nếu DTO có (không bắt buộc)
     * <p>
     * Cách này dùng reflection để tránh phụ thuộc cứng vào tên field cụ thể trong dự án của bạn.
     */
    private RoleDTOs.CreateRoleReq buildDefaultCreateRoleReq(PlayerSession ps) {
        RoleDTOs.CreateRoleReq req = new RoleDTOs.CreateRoleReq();

        // userId
        trySetString(req, "setUserId", ps.getUserId());
        // fallback nếu DTO dùng tên khác:
        trySetString(req, "setUid", ps.getUserId());
        trySetString(req, "setAccountId", ps.getUserId());

        // tên hiển thị
        String roleName = defaultRoleName(ps.getUsername(), ps.getUserId());
        trySetString(req, "setNickname", roleName);
        trySetString(req, "setRoleName", roleName);
        trySetString(req, "setName", roleName);

        // (Optional) set các default khác nếu DTO có: job/class/gender/serverId...
        trySetInt(req, "setJob", 1);
        trySetInt(req, "setClassId", 1);
        trySetInt(req, "setGender", 0);
        trySetString(req, "setServerId", "s1");

        return req;
    }

    private static void trySetString(Object obj, String method, String value) {
        try {
            var m = obj.getClass().getMethod(method, String.class);
            m.invoke(obj, value);
        } catch (Throwable ignore) {
        }
    }


    private Mono<Void> bootstrapAfterLogin(String token, PlayerSession ps) {
        String rid = ps.getRoleId();

        Mono<Void> bagMono = Mono.defer(() ->
                        FeignCall.withToken(token, "bag.get", () -> bagPublicFeign.get(rid, BAG_COMMON))
                )
                .doOnSubscribe(s -> log.debug("[boot] bag.get subscribe role={}", rid))
                .doOnNext(bag -> sendKnapsackAll(ps, bag))
                // Nếu bạn muốn bag là bắt buộc, bỏ onErrorResume. Nếu chỉ cảnh báo, giữ như dưới:
                .onErrorResume(ex -> { log.warn("[boot] bag.get err: {}", ex.toString()); return Mono.empty(); })
                .then();

        Mono<Void> equipMono = Mono.defer(() ->
                        FeignCall.withToken(token, "equip.list", () -> equipFeign.list(rid))
                )
                .doOnSubscribe(s -> log.debug("[boot] equip.list subscribe role={}", rid))
                .doOnNext(eqList -> sendEquipList(ps, eqList))
                .onErrorResume(ex -> { log.warn("[boot] equip.list err: {}", ex.toString()); return Mono.empty(); })
                .then();

        Mono<Void> equipBagMono = Mono.defer(() ->
                        FeignCall.withToken(token, "bag.get.equip", () -> bagPublicFeign.get(rid, BAG_EQUIP))
                )
                .doOnSubscribe(s -> log.debug("[boot] bag.get.equip subscribe role={}", rid))
                .doOnNext(ebag -> sendEquipBagList(ps, ebag))
                .onErrorResume(ex -> { log.info("[boot] equip bag optional: {}", ex.getMessage()); return Mono.empty(); })
                .then();

        Mono<Void> walletMono = Mono.fromRunnable(() -> refreshWalletAndEmit(ps))
                .doOnSubscribe(s -> log.debug("[boot] wallet.refresh subscribe role={}", rid))
                .subscribeOn(Schedulers.boundedElastic()) // tránh block event-loop nếu có gọi Feign hoặc I/O
                .onErrorResume(ex -> { log.warn("[boot] wallet err: {}", ex.getMessage()); return Mono.empty(); })
                .then();

        Mono<Void> boxMono = Mono.defer(() ->
                        FeignCall.withToken(token, "box.info", () -> boxFeign.info(rid))
                )
                .doOnSubscribe(s -> log.debug("[boot] box.info subscribe role={}", rid))
                .doOnNext(info -> sendBoxInfo(ps, info))
                .onErrorResume(ex -> { log.info("[boot] box info optional: {}", ex.getMessage()); return Mono.empty(); })
                .then();

        Mono<Void> fumoMono = Mono.defer(() ->
                        FeignCall.withToken(token, "fumo.list", () -> equipFumoFeign.list(rid))
                )
                .doOnSubscribe(s -> log.debug("[boot] fumo.list subscribe role={}", rid))
                .doOnNext(r -> sendEquipFumoList(ps, r == null ? List.of() : r.fumoList()))
                .onErrorResume(ex -> { log.info("[boot] fumo list optional: {}", ex.getMessage()); return Mono.empty(); })
                .then();

        Mono<Void> shopMono = Mono.fromRunnable(() -> sendShopInfo(ps))
                .doOnSubscribe(s -> log.debug("[boot] shop.info subscribe role={}", rid))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> { log.info("[boot] shop info optional: {}", ex.getMessage()); return Mono.empty(); })
                .then();


        return Mono.whenDelayError(bagMono, equipMono, equipBagMono, walletMono, boxMono, fumoMono, shopMono)
                .doOnSubscribe(s -> log.info("[boot] START role={}", rid))
                .doOnTerminate(() -> log.info("[boot] DONE role={}", rid))
                .checkpoint("bootstrapAfterLogin");
    }

    private Mono<Void> sendLoginAck(PlayerSession ps, int result, int forbid) {
        var ack = Msglogin.PB_SCLoginToAccount.newBuilder()
                .setResult(result)
                .setForbidTime(forbid)
                .build();
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_LOGIN_ACK, ack.toByteArray()));
        return Mono.empty();
    }

    // =============================================================================================
    //  Knapsack (1500) — tích hợp gift-service vào USE
    // =============================================================================================

    private Mono<Void> onKnapsack(PlayerSession ps, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        try {
            var req = Msgknapsack.PB_CSKnapsackReq.parseFrom(payload);
            int type = req.hasReqType() ? req.getReqType() : -1;
            List<Integer> param = req.getParamList();

            switch (type) {
                // USE = 0, param=[itemId, num]
                case 0 -> {
                    int itemId = (param.size() >= 1) ? param.get(0) : 0;
                    long numL = (param.size() >= 2) ? param.get(1) : 1;
                    if (itemId <= 0 || numL <= 0) return Mono.empty();
                    int num = (int) Math.min(numL, Integer.MAX_VALUE);

                    boolean handledByGift = false;
                    try {
                        // Thử mở quà qua gift-service trước
                        var greq = GiftDTOs.OpenReq.builder()
                                .roleId(ps.getRoleId())
                                .giftItemId(itemId)
                                .count(num)
                                .bagType(BAG_COMMON)
                                .build();
                        var gresp = giftFeign.open(greq);

                        if (gresp != null && Boolean.TRUE.equals(gresp.isOk())) {
                            handledByGift = true;
                            // gift-service đã tự consume hộp + cộng item/coin; chỉ cần refresh túi/ ví
                            safeRefreshKnapsack(ps);
                            refreshWalletAndEmit(ps);

                            // (optional) nếu bạn có SC thông báo phần thưởng thì emit ở đây
                        } else if (gresp != null && "GIFT_NOT_FOUND".equalsIgnoreCase(gresp.getError())) {
                            handledByGift = false; // fallback
                        } else if (gresp != null) {
                            // lỗi khác: vẫn refresh để đồng bộ
                            handledByGift = true;
                            safeRefreshKnapsack(ps);
                            refreshWalletAndEmit(ps);
                        }
                    } catch (Exception ex) {
                        // gift-service unreachable hoặc lỗi bất ngờ -> fallback
                        handledByGift = false;
                        log.warn("gift open failed, fallback to bag consume. roleId={}, itemId={}, ex={}",
                                ps.getRoleId(), itemId, ex.toString());
                    }

                    if (!handledByGift) {
                        // Fallback: consume trực tiếp trong túi như logic cũ
                        BagDTOs.ConsumeReq consume = new BagDTOs.ConsumeReq(
                                ps.getRoleId(), BAG_COMMON,
                                List.of(new BagDTOs.ItemDelta(itemId, num, false, null)),
                                1500, 0
                        );
                        var ok = bagInternalFeign.consume(consume);
                        if (ok != null && ok.ok()) {
                            var bag = bagPublicFeign.get(ps.getRoleId(), BAG_COMMON);
                            sendKnapsackSingle(ps, bag, itemId);
                            // refresh ví đề phòng item USE cộng tiền
                            refreshWalletAndEmit(ps);
                        } else {
                            var notice = Msgknapsack.PB_SCItemNotEnoughNotice.newBuilder()
                                    .setItemId(itemId).build();
                            ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_ITEM_NOT_ENOUGH, notice.toByteArray()));
                        }
                    }
                }
                default -> log.debug("PB_CSKnapsackReq type={} not implemented", type);
            }
        } catch (Exception e) {
            log.warn("1500 error: {}", e.getMessage());
        }
        return Mono.empty();
    }

    private void sendKnapsackAll(PlayerSession ps, BagDTOs.BagView bag) {
        if (bag == null || bag.slots() == null) return;
        var b = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
        for (var s : bag.slots()) {
            if (s.count() <= 0) continue;
            b.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                    .setItemId(s.itemId())
                    .setNum(s.count())
                    .build());
        }
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_KNAPSACK_ALL_INFO, b.build().toByteArray()));
    }

    private void sendKnapsackSingle(PlayerSession ps, BagDTOs.BagView bag, int itemId) {
        long total = 0;
        if (bag != null && bag.slots() != null) {
            for (var s : bag.slots()) if (s.itemId() == itemId) total += s.count();
        }
        var single = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                .setItem(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(itemId)
                        .setNum(total)
                        .build())
                .build();
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_KNAPSACK_SINGLE_INFO, single.toByteArray()));
    }

    // =============================================================================================
    //  Equip (1600) + FUMO
    // =============================================================================================

    private Mono<Void> onEquip(PlayerSession ps, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        try {
            var req = Msgequip.PB_CSEquipReq.parseFrom(payload);
            int type = req.hasReqType() ? req.getReqType() : -1;
            int p1 = req.hasParam1() ? req.getParam1() : 0;
            int p2 = req.hasParam2() ? req.getParam2() : 0;
            int p3 = req.hasParam3() ? req.getParam3() : 0;

            switch (type) {
                case EquipOp.WEAR -> {
                    return handleWear(ps, p1, p2);
                }
                case EquipOp.FUMO_LIST -> {
                    var r = equipFumoFeign.list(ps.getRoleId());
                    sendEquipFumoList(ps, r == null ? List.of() : r.fumoList());
                }
                case EquipOp.FUMO_ONE -> {
                    int equipType = p1;
                    var r = equipFumoFeign.one(ps.getRoleId(), equipType);
                    sendEquipFumoOne(ps, equipType, r == null ? null : r.fumoData());
                }
                case EquipOp.FUMO_ADD_EXP -> {
                    int equipType = p1;
                    int addExp = Math.max(0, p2);
                    var r = equipFumoFeign.addExp(new EquipFumoDTOs.AddExpReq(
                            ps.getRoleId(), equipType, addExp, null
                    ));
                    sendEquipFumoOne(ps, equipType, r == null ? null : r.fumoData());
                }
                case EquipOp.FUMO_ACTIVATE -> {
                    int equipType = p1;
                    long endEpoch = (p3 > 0) ? p3 : (Instant.now().getEpochSecond() + Math.max(0, p2));
                    var r = equipFumoFeign.activate(new EquipFumoDTOs.ActivateReq(
                            ps.getRoleId(), equipType, endEpoch
                    ));
                    sendEquipFumoOne(ps, equipType, r == null ? null : r.fumoData());
                }
                case EquipOp.FUMO_RESET -> {
                    int equipType = p1;
                    var ok = equipFumoFeign.reset(new EquipFumoDTOs.ResetReq(
                            ps.getRoleId(), equipType, null
                    ));
                    if (ok != null && ok.ok()) {
                        sendEquipFumoOne(ps, equipType, new EquipFumoDTOs.FumoData(0, 0, 0));
                    }
                }
                default -> log.debug("PB_CSEquipReq type={} not implemented", type);
            }
        } catch (Exception e) {
            log.warn("1600 error: {}", e.getMessage());
        }
        return Mono.empty();
    }

    // ==== WEAR tách riêng cho gọn
    private Mono<Void> handleWear(PlayerSession ps, int itemId, int forceEquipTypeMaybe) {
        try {
            var resp = equipFeign.equip(EquipDTOs.EquipReq.builder()
                    .roleId(ps.getRoleId())
                    .itemId(itemId)
                    .bagType(BAG_EQUIP)
                    .forceEquipType((byte) forceEquipTypeMaybe)
                    .build());
            if (resp != null && resp.ok()) {
                var eqList = equipFeign.list(ps.getRoleId());
                sendEquipList(ps, eqList);
                try {
                    var ebag = bagPublicFeign.get(ps.getRoleId(), BAG_EQUIP);
                    sendEquipBagList(ps, ebag);
                } catch (Exception ex) {
                    log.info("equip bag not available: {}", ex.getMessage());
                }
            } else {
                log.debug("equip fail: {}", resp == null ? "null" : resp.message());
            }
        } catch (Exception ex) {
            log.warn("handleWear error: {}", ex.getMessage());
        }
        return Mono.empty();
    }

    private void sendEquipList(PlayerSession ps, EquipDTOs.ListResp list) {
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
                b.addEquipList(eb.build());
            }
        }
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_EQUIP_LIST_INFO, b.build().toByteArray()));
    }

    private void sendEquipBagList(PlayerSession ps, BagDTOs.BagView bag) {
        if (bag == null || bag.slots() == null) {
            var empty = Msgequip.PB_SCEquipBagListInfo.newBuilder().build();
            ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_EQUIP_BAG_LIST_INFO, empty.toByteArray()));
            return;
        }
        var b = Msgequip.PB_SCEquipBagListInfo.newBuilder();
        for (var s : bag.slots()) {
            var ed = Msgequip.PB_EquipData.newBuilder()
                    .setItemId(s.itemId())
                    .setEquipType(0) // TODO: map equipType từ meta nếu cần
                    .build();
            var bd = Msgequip.PB_EquipBagData.newBuilder()
                    .setIndex(s.slotIndex())
                    .setBagData(ed)
                    .build();
            b.addBagList(bd);
        }
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_EQUIP_BAG_LIST_INFO, b.build().toByteArray()));
    }

    // =============================================================================================
    //  Helpers
    // =============================================================================================

    private void emit(PlayerSession ps, int msgId, byte[] body) {
        try {
            ps.getOutbound().tryEmitNext(PacketCodec.encode(msgId, body));
        } catch (Throwable t) {
            log.warn("emit failed. roleId={}, msgId={}, ex={}", ps.getRoleId(), msgId, t.toString());
        }
    }

    /**
     * Refresh toàn bộ túi cho client (khuyến cáo gọi sau các thao tác có thể làm thay đổi item)
     * Hàm này cố gắng không throw ra ngoài để không chặn pipeline.
     */
    private Mono<Void> safeRefreshKnapsack(PlayerSession ps) {
        if (ps == null || !StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        String token = ps.getSessionId();

        // Nếu bạn đã có wrapper FeignCall.withToken(...) trả Mono<T> (khuyên dùng)
        return FeignCall.withToken(token, "bag.get",
                        () -> bagPublicFeign.get(ps.getRoleId(), BAG_COMMON))
                .doOnSubscribe(s -> log.debug("[bag] get subscribe role={}", ps.getRoleId()))
                .doOnNext(bag -> {
                    try {
                        sendKnapsackAll(ps, bag);   // map sang SC_KNAPSACK_ALL_INFO
                    } catch (Throwable t) {
                        log.warn("[bag] sendKnapsackAll err: {}", t.getMessage());
                    }
                })
                .doOnError(ex -> log.info("[bag] get optional error: {}", ex.getMessage()))
                .onErrorResume(ex -> Mono.empty())
                .then();
    }

    /**
     * Refresh ví (tiền tệ) và bắn PB_SCKnapsackSingleInfo cho các item ảo (gold/diamond/silver..)
     * (dùng GET /internal/wallet/{roleId}?itemIds=...)
     */
    private void refreshWalletAndEmit(PlayerSession ps) {
        if (!StringUtils.hasText(ps.getRoleId())) return;
        try {
            var itemIds = new ArrayList<>(DEFAULT_WALLET_IDS);
            var resp = walletFeign.get(ps.getRoleId(), itemIds); // ResultDTO<BalancesResp>
            if (resp != null && resp.getCode() == 0 && resp.getData() != null && resp.getData().balances() != null) {
                resp.getData().balances().forEach((itemId, total) ->
                        sendKnapsackSingleDirect(ps, itemId.intValue(), total == null ? 0L : total));
            } else {
                String err = (resp == null) ? "NULL_RESPONSE"
                        : (resp.getMessage() != null ? resp.getMessage() : "UNKNOWN");
                log.warn("refreshWalletAndEmit: roleId={}, err={}", ps.getRoleId(), err);
            }
        } catch (Exception ex) {
            log.warn("refreshWalletAndEmit error roleId={}, ex={}", ps.getRoleId(), ex.toString());
        }
    }

    /**
     * Emit SC_KNAPSACK_SINGLE_INFO cho một itemId với số lượng 'total' đã biết sẵn.
     */
    private void sendKnapsackSingleDirect(PlayerSession ps, int itemId, long total) {
        var single = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                .setItem(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(itemId)
                        .setNum(Math.max(0, total))
                        .build())
                .build();
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_KNAPSACK_SINGLE_INFO, single.toByteArray()));
    }

    /**
     * Trừ ví (virtual items) — dùng khi bạn muốn thao tác ngay tại WS.
     */
    private WalletDTOs.MutateResp walletBatchCost(
            String roleId,
            Map<Long, Long> deltas,
            String idemKey,
            int reason,
            int reasonType
    ) {
        var changes = deltas.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .map(e -> new WalletDTOs.Change(e.getKey(), e.getValue()))
                .toList();

        long now = Instant.now().getEpochSecond();
        if (changes.isEmpty()) {
            return new WalletDTOs.MutateResp(true, null, Map.of(), now);
        }

        try {
            var req = new WalletDTOs.BatchReq(roleId, changes, idemKey, reason, reasonType);
            var resp = walletFeign.batchCost(req); // ResultDTO<MutateResp>
            if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
                return resp.getData();
            }
            String err = (resp == null) ? "NULL_RESPONSE"
                    : (resp.getMessage() != null ? resp.getMessage() : "UNKNOWN");
            return new WalletDTOs.MutateResp(false, err, Map.of(), now);
        } catch (Exception ex) {
            return new WalletDTOs.MutateResp(false, ex.getMessage(), Map.of(), now);
        }
    }


    /**
     * Cộng ví (virtual items) — dùng khi bạn muốn thao tác ngay tại WS.
     */
    private WalletDTOs.MutateResp walletBatchAdd(
            String roleId,
            Map<Long, Long> deltas,
            String idemKey,
            int reason,
            Integer reasonType
    ) {
        var changes = deltas.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .map(e -> new WalletDTOs.Change(e.getKey(), e.getValue()))
                .toList();

        long now = Instant.now().getEpochSecond();
        if (changes.isEmpty()) {
            return new WalletDTOs.MutateResp(true, null, Map.of(), now);
        }

        try {
            var req = new WalletDTOs.BatchReq(roleId, changes, idemKey, reason, reasonType);
            var resp = walletFeign.batchAdd(req); // ResultDTO<MutateResp>
            if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
                return resp.getData();
            }
            String err = (resp == null) ? "NULL_RESPONSE"
                    : (resp.getMessage() != null ? resp.getMessage() : "UNKNOWN");
            return new WalletDTOs.MutateResp(false, err, Map.of(), now);
        } catch (Exception ex) {
            return new WalletDTOs.MutateResp(false, ex.getMessage(), Map.of(), now);
        }
    }

    /**
     * Refresh danh sách trang bị đang mặc/đã sở hữu để client cập nhật UI
     */
    private Mono<Void> safeRefreshEquipList(PlayerSession ps) {
        if (ps == null || !StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        String token = ps.getSessionId();

        return FeignCall.withToken(token, "equip.list",
                        () -> equipFeign.list(ps.getRoleId()))
                .doOnSubscribe(s -> log.debug("[equip] list subscribe role={}", ps.getRoleId()))
                .doOnNext(list -> {
                    try {
                        sendEquipList(ps, list);
                    } catch (Throwable t) {
                        log.warn("[equip] sendEquipList err: {}", t.getMessage());
                    }
                })
                .doOnError(ex -> log.info("[equip] list optional error: {}", ex.getMessage()))
                .onErrorResume(ex -> Mono.empty())
                .then();
    }
    private static String first(HttpHeaders h, String key) {
        var v = h.getFirst(key);
        return (v == null || v.isBlank()) ? null : v;
    }

    // uint32 clamp cho end_time
    private static int safeU32(long v) {
        if (v < 0) return 0;
        return (int) Math.min(v, 0xffff_ffffL);
    }

    private void sendEquipFumoList(PlayerSession ps, List<EquipFumoDTOs.FumoData> fumos) {
        var b = Msgequip.PB_SCEquipFuMoListInfo.newBuilder();
        if (fumos != null) {
            for (var f : fumos) {
                b.addFumoList(Msgequip.PB_EquipFuMoData.newBuilder()
                        .setLevel(f.level())
                        .setExp(f.exp())
                        .setEndTime(safeU32(f.endTimeEpochSec()))
                        .build());
            }
        }
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_EQUIP_FUMO_LIST, b.build().toByteArray()));
    }

    private void sendEquipFumoOne(PlayerSession ps, int equipType, EquipFumoDTOs.FumoData f) {
        var b = Msgequip.PB_SCEquipFuMoOneInfo.newBuilder()
                .setEquipType(equipType);
        if (f != null) {
            b.setFumoData(Msgequip.PB_EquipFuMoData.newBuilder()
                    .setLevel(f.level())
                    .setExp(f.exp())
                    .setEndTime(safeU32(f.endTimeEpochSec()))
                    .build());
        }
        ps.getOutbound().tryEmitNext(PacketCodec.encode(MsgIds.SC_EQUIP_FUMO_ONE, b.build().toByteArray()));
    }

    /**
     * Gửi thông tin Shop cho client khởi tạo UI.
     * - Không phụ thuộc vào schema cụ thể của PB_SCShopInfo (dùng reflection set các mốc thời gian nếu có).
     * - Nếu bạn có API shopFeign.info()/list() riêng, có thể map chi tiết ở đây (xem TODO).
     */
    private void sendShopInfo(PlayerSession ps) {
        if (!StringUtils.hasText(ps.getRoleId())) return;

        try {
            var b = Msgother.PB_SCShopInfo.newBuilder();

            // ====== (Optional) set các mốc thời gian nếu PB có các setter tương ứng ======
            long now = Instant.now().getEpochSecond();
            // Ví dụ: nếu .proto có int32 refresh_time / timestamp / next_refresh_time
            trySetInt(b, "setRefreshTime", (int) now);
            trySetInt(b, "setTimestamp", (int) now);
            trySetInt(b, "setNextRefreshTime", (int) (now + 3600)); // giả định refresh sau 1h

            // ====== (Optional) nếu bạn có API shopFeign.info()/list(), map chi tiết tại đây ======
            // TODO: Nếu có DTO cụ thể (vd: ShopDTOs.InfoResp), bạn có thể:
            // var info = shopFeign.info(ShopDTOs.Kind.COMMON); // ví dụ
            // - Đọc danh sách hàng hoá / thời gian refresh từ info
            // - Dùng các setter tương ứng của PB_SCShopInfo để add vào builder (tránh reflection khi đã biết chắc schema)
            // Lưu ý: do không có schema chi tiết PB_SCShopInfo trong ngữ cảnh hiện tại,
            // đoạn mapping cụ thể không thể giả định chính xác tên field -> nên để TODO ở đây.

            emit(ps, MsgIds.SC_SHOP_INFO, b.build().toByteArray());
        } catch (Exception ex) {
            log.info("sendShopInfo optional error: {}", ex.getMessage());
        }
    }

    /**
     * Thử gọi một setter int (vd: setRefreshTime(int)) bằng reflection nếu tồn tại.
     * Không ném lỗi ra ngoài để không làm gián đoạn pipeline.
     */
    private static void trySetInt(Object builder, String methodName, int value) {
        try {
            var m = builder.getClass().getMethod(methodName, int.class);
            m.invoke(builder, value);
        } catch (Throwable ignore) {
            // Không sao: setter không tồn tại hoặc kiểu không khớp -> bỏ qua
        }
    }
}