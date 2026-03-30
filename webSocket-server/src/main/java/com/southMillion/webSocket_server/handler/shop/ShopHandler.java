package com.SouthMillion.webSocket_server.handler.shop;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.service.client.ShopFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Shop Handler - P1a Priority
 * Handles shop operations: list, info, buy
 * 
 * MsgIds: 1620 (common buy), 1622 (cloth buy), 1630 (mystery shop)
 * Operations:
 *   0: Get shop info
 *   1: List common shop items
 *   2: List cloth shop items
 *   3: List mystery shop items
 *   4: Buy item
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopHandler implements MessageHandler {

    private final ShopFeign shopFeign;
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;
    private final RoleFeign roleFeign;
    private final TaskProgressPublisher taskProgressPublisher;

    // Operation types
    private static final int OP_GET_INFO = 0;      // Get shop info
    private static final int OP_LIST_COMMON = 1;   // List common items
    private static final int OP_LIST_CLOTH = 2;    // List cloth items
    private static final int OP_LIST_MYSTERY = 3;  // List mystery items
    private static final int OP_BUY = 4;           // Buy item

    // Mystery shop operation types
    private static final int MYSTERY_OP_REFRESH = 0;  // Refresh/list mystery shop
    private static final int MYSTERY_OP_BUY = 1;      // Buy mystery item

    @Override
    public int[] interests() {
        return new int[]{1620, 1622, 1630};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                
                log.debug("[Shop] Handler msgId={}, roleId={}", msgId, roleId);
                
                switch (msgId) {
                    case 1620 -> handleBuyCommon(session, roleId, payload);
                    case 1622 -> handleBuyCloth(session, roleId, payload);
                    case 1630 -> handleMysteryShop(session, roleId, payload);
                    default -> log.warn("[Shop] Unknown msgId: {}", msgId);
                }
                
            } catch (Exception e) {
                log.error("[Shop] Error handling request for roleId={}, msgId={}", 
                    session.getRoleId(), msgId, e);
                sendErrorResponse(session);
            }
        });
    }

    /**
     * OP 0: Get shop info (mystery shop refresh time, slots)
     */
    private void handleGetInfo(PlayerSession session, Long roleId, int level) {
        try {
            log.debug("[Shop] Getting shop info for roleId={}, level={}", roleId, level);
            
            ResultDTO<ShopDTOs.InfoResp> result = shopFeign.info(String.valueOf(roleId), level, null);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.InfoResp data = result.getData();
                
                Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
                // Add shop data if available
                if (data.getItems() != null) {
                    for (ShopDTOs.ShopItem item : data.getItems()) {
                        Msgother.PB_ShopData.Builder shopData = Msgother.PB_ShopData.newBuilder();
                        if (item.idOrIndex() != null) {
                            try {
                                shopData.setIndex(Integer.parseInt(item.idOrIndex()));
                            } catch (NumberFormatException e) {
                                log.warn("[Shop] Invalid item index: {}", item.idOrIndex());
                            }
                        }
                        response.addDataList(shopData);
                    }
                }
                
                sendShopInfoResponse(session, response.build());
            } else {
                log.warn("[Shop] Failed to get shop info: {}", result.error());
                sendErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error getting shop info for roleId={}", roleId, e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP 1: List common shop items
     */
    private void handleListCommon(PlayerSession session, Long roleId, int level, int page) {
        try {
            log.debug("[Shop] Listing common items for level={}, page={}", level, page);
            
            ResultDTO<ShopDTOs.ShopListResp> result = shopFeign.listCommon(
                    new ShopDTOs.ListCommonReq(String.valueOf(roleId), level, page, 0));
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.ShopListResp data = result.getData();
                
                Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
                if (data.items() != null) {
                    for (ShopDTOs.ShopItem item : data.items()) {
                        Msgother.PB_ShopData.Builder shopData = Msgother.PB_ShopData.newBuilder();
                        if (item.idOrIndex() != null) {
                            try {
                                shopData.setIndex(Integer.parseInt(item.idOrIndex()));
                            } catch (NumberFormatException e) {
                                log.warn("[Shop] Invalid item index: {}", item.idOrIndex());
                            }
                        }
                        response.addDataList(shopData);
                    }
                }
                
                sendShopInfoResponse(session, response.build());
            } else {
                log.warn("[Shop] Failed to list common items: {}", result.error());
                sendErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error listing common items", e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP 2: List cloth shop items
     */
    private void handleListCloth(PlayerSession session, Long roleId, int level, int page) {
        try {
            log.debug("[Shop] Listing cloth items for level={}, page={}", level, page);
            
            ShopDTOs.ListClothReq req = new ShopDTOs.ListClothReq(String.valueOf(roleId), level, page);
            ResultDTO<ShopDTOs.ShopListResp> result = shopFeign.listCloth(req);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.ShopListResp data = result.getData();
                
                Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
                if (data.items() != null) {
                    for (ShopDTOs.ShopItem item : data.items()) {
                        Msgother.PB_ShopData.Builder shopData = Msgother.PB_ShopData.newBuilder();
                        if (item.idOrIndex() != null) {
                            try {
                                shopData.setIndex(Integer.parseInt(item.idOrIndex()));
                            } catch (NumberFormatException e) {
                                log.warn("[Shop] Invalid item index: {}", item.idOrIndex());
                            }
                        }
                        response.addDataList(shopData);
                    }
                }
                
                sendShopInfoResponse(session, response.build());
            } else {
                log.warn("[Shop] Failed to list cloth items: {}", result.error());
                sendErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error listing cloth items", e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP 3: List mystery shop items
     */
    private void handleListMystery(PlayerSession session, Long roleId, Integer slots) {
        try {
            int level = 1;
            try {
                OtherRoleDTOs.OtherRoleInfo roleInfo = roleFeign.getOtherRole(
                        session.getUserId(), String.valueOf(roleId));
                if (roleInfo != null && roleInfo.attributes() != null && roleInfo.attributes().level() > 0) {
                    level = roleInfo.attributes().level();
                }
            } catch (Exception ex) {
                log.warn("[Shop/Mystery] Cannot get player level for roleId={}, fallback to 1: {}",
                        roleId, ex.getMessage());
            }
            log.debug("[Shop] Listing mystery items for level={}, slots={}", level, slots);
            
            ResultDTO<ShopDTOs.ShopListResp> result = shopFeign.listMystery(level, slots);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.ShopListResp data = result.getData();
                
                Msgother.PB_SCMysteryShopInfo.Builder response = Msgother.PB_SCMysteryShopInfo.newBuilder();
                if (data.items() != null) {
                    for (ShopDTOs.ShopItem item : data.items()) {
                        if (item.idOrIndex() != null) {
                            try {
                                response.addIndexList(Integer.parseInt(item.idOrIndex()));
                            } catch (NumberFormatException e) {
                                log.warn("[Shop] Invalid item index: {}", item.idOrIndex());
                            }
                        }
                    }
                }
                
                sendMysteryShopInfoResponse(session, response.build());
            } else {
                log.warn("[Shop] Failed to list mystery items: {}", result.error());
                sendMysteryShopErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error listing mystery items", e);
            sendMysteryShopErrorResponse(session);
        }
    }

    /**
     * Handle buy common shop item (msgId 1620)
     */
    private void handleBuyCommon(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSShopBuyReq req = Msgother.PB_CSShopBuyReq.parseFrom(payload);
            int index = req.hasIndex() ? req.getIndex() : 0;
            int num = req.hasNum() ? req.getNum() : 1;
            
            log.debug("[Shop] Buying common item index={}, num={}, roleId={}", index, num, roleId);
            
            ShopDTOs.BuyReq buyReq = new ShopDTOs.BuyReq(
                String.valueOf(roleId), 
                ShopDTOs.Kind.COMMON, 
                index, 
                num, 
                0, // receiveBagType
                0  // walletBagType
            );
            
            ResultDTO<ShopDTOs.BuyResp> result = shopFeign.buy(buyReq);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.BuyResp data = result.getData();
                
                syncPostPurchaseState(session, roleId, data);

                Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
                Msgother.PB_ShopData.Builder shopData = Msgother.PB_ShopData.newBuilder();
                shopData.setIndex(index);
                shopData.setBuyNum(num);
                response.addDataList(shopData);
                
                sendShopInfoResponse(session, response.build());
                log.info("[Shop] Successfully bought common item index={}, cost={}, remaining={}", 
                    index, data.getCost(), data.getRemainingBalance());
            } else {
                log.warn("[Shop] Failed to buy common item: {}", result.error());
                sendErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error buying common item for roleId={}", roleId, e);
            sendErrorResponse(session);
        }
    }

    /**
     * Handle buy cloth shop item (msgId 1622)
     */
    private void handleBuyCloth(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSClothShopBuyReq req = Msgother.PB_CSClothShopBuyReq.parseFrom(payload);
            int seq = req.hasSeq() ? req.getSeq() : 0;
            int num = req.hasNum() ? req.getNum() : 1;
            
            log.debug("[Shop] Buying cloth item seq={}, num={}, roleId={}", seq, num, roleId);
            
            ShopDTOs.BuyReq buyReq = new ShopDTOs.BuyReq(
                String.valueOf(roleId), 
                ShopDTOs.Kind.CLOTH, 
                seq, 
                num, 
                0, // receiveBagType
                0  // walletBagType
            );
            
            ResultDTO<ShopDTOs.BuyResp> result = shopFeign.buy(buyReq);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.BuyResp data = result.getData();
                
                syncPostPurchaseState(session, roleId, data);

                Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
                Msgother.PB_ShopData.Builder shopData = Msgother.PB_ShopData.newBuilder();
                shopData.setIndex(seq);
                shopData.setBuyNum(num);
                response.addDataList(shopData);
                
                sendShopInfoResponse(session, response.build());
                log.info("[Shop] Successfully bought cloth item seq={}, cost={}, remaining={}", 
                    seq, data.getCost(), data.getRemainingBalance());
            } else {
                log.warn("[Shop] Failed to buy cloth item: {}", result.error());
                sendErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error buying cloth item for roleId={}", roleId, e);
            sendErrorResponse(session);
        }
    }

    /**
     * Handle mystery shop operations (msgId 1630)
     */
    private void handleMysteryShop(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSMysteryShopReq req = Msgother.PB_CSMysteryShopReq.parseFrom(payload);
            int opType = req.hasOpType() ? req.getOpType() : 0;
            int param = req.hasParam() ? req.getParam() : 0;
            
            log.debug("[Shop] Mystery shop op={}, param={}, roleId={}", opType, param, roleId);
            
            switch (opType) {
                case MYSTERY_OP_REFRESH -> handleListMystery(session, roleId, param);
                case MYSTERY_OP_BUY -> handleBuyMystery(session, roleId, param);
                default -> {
                    log.warn("[Shop] Unknown mystery shop operation: {}", opType);
                    sendMysteryShopErrorResponse(session);
                }
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error handling mystery shop for roleId={}", roleId, e);
            sendMysteryShopErrorResponse(session);
        }
    }

    /**
     * Handle buy mystery shop item
     */
    private void handleBuyMystery(PlayerSession session, Long roleId, int index) {
        try {
            log.debug("[Shop] Buying mystery item index={}, roleId={}", index, roleId);
            
            ShopDTOs.BuyReq buyReq = new ShopDTOs.BuyReq(
                String.valueOf(roleId), 
                ShopDTOs.Kind.SHENMI, 
                index, 
                1, // Mystery shop typically allows buying 1 at a time
                0, // receiveBagType
                0  // walletBagType
            );
            
            ResultDTO<ShopDTOs.BuyResp> result = shopFeign.buy(buyReq);
            
            if (result.isSuccess() && result.getData() != null) {
                ShopDTOs.BuyResp data = result.getData();
                
                syncPostPurchaseState(session, roleId, data);

                Msgother.PB_SCMysteryShopInfo.Builder response = Msgother.PB_SCMysteryShopInfo.newBuilder();
                // Set buy flag with bit for this index
                response.setBuyFlag(1 << index);
                
                sendMysteryShopInfoResponse(session, response.build());
                log.info("[Shop] Successfully bought mystery item index={}, cost={}, remaining={}", 
                    index, data.getCost(), data.getRemainingBalance());
            } else {
                log.warn("[Shop] Failed to buy mystery item: {}", result.error());
                sendMysteryShopErrorResponse(session);
            }
            
        } catch (Exception e) {
            log.error("[Shop] Error buying mystery item for roleId={}", roleId, e);
            sendMysteryShopErrorResponse(session);
        }
    }

    // Response sending methods
    private void sendShopInfoResponse(PlayerSession session, Msgother.PB_SCShopInfo response) {
        try {
            byte[] responseBytes = response.toByteArray();
            byte[] packet = PacketCodec.encode(1621, responseBytes); // PB_SCShopInfo msgId
            session.sendBinary(packet);
        } catch (Exception e) {
            log.error("[Shop] Failed to send shop info response", e);
        }
    }

    private void sendMysteryShopInfoResponse(PlayerSession session, Msgother.PB_SCMysteryShopInfo response) {
        try {
            byte[] responseBytes = response.toByteArray();
            byte[] packet = PacketCodec.encode(1631, responseBytes); // PB_SCMysteryShopInfo msgId
            session.sendBinary(packet);
        } catch (Exception e) {
            log.error("[Shop] Failed to send mystery shop info response", e);
        }
    }

    private void sendErrorResponse(PlayerSession session) {
        try {
            Msgother.PB_SCShopInfo.Builder response = Msgother.PB_SCShopInfo.newBuilder();
            sendShopInfoResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Shop] Failed to send error response", e);
        }
    }

    private void sendMysteryShopErrorResponse(PlayerSession session) {
        try {
            Msgother.PB_SCMysteryShopInfo.Builder response = Msgother.PB_SCMysteryShopInfo.newBuilder();
            sendMysteryShopInfoResponse(session, response.build());
        } catch (Exception e) {
            log.error("[Shop] Failed to send mystery shop error response", e);
        }
    }

    /**
     * Keep push order deterministic after a successful shop mutation:
     * 1) sync affected bag item count, 2) sync wallet balances, 3) report spend task.
     */
    private void syncPostPurchaseState(PlayerSession session, Long roleId, ShopDTOs.BuyResp data) {
        long rewardItemId = firstRewardItemId(data);
        if (rewardItemId > 0) {
            pushBagItemCount(session, roleId, (int) rewardItemId);
        }
        pushWalletBalance(session, roleId);
        reportSpendGoldTask(roleId, data != null ? data.getCost() : 0L);
    }

    private long firstRewardItemId(ShopDTOs.BuyResp data) {
        if (data == null || data.getItems() == null || data.getItems().isEmpty()) {
            return 0L;
        }
        for (ShopDTOs.ShopItem item : data.getItems()) {
            if (item != null && item.rewardItemId() > 0) {
                return item.rewardItemId();
            }
        }
        return 0L;
    }

    private void pushBagItemCount(PlayerSession session, Long roleId, int itemId) {
        if (itemId <= 0) {
            return;
        }
        try {
            List<BagDTOs.ItemView> list = bagFeign.list(String.valueOf(roleId));
            long amount = 0L;
            if (list != null) {
                amount = list.stream()
                        .filter(iv -> iv != null && iv.getItemId() != null && iv.getItemId() == itemId)
                        .map(iv -> iv.getNum() == null ? 0L : iv.getNum().longValue())
                        .mapToLong(Long::longValue)
                        .sum();
            }
            Emitters.sendKnapsackSingleInfo(session, itemId, amount);
        } catch (Exception e) {
            log.warn("[Shop] Failed to push bag single item for roleId={}, itemId={}: {}", roleId, itemId, e.getMessage());
        }
    }

    private void reportSpendGoldTask(Long roleId, long cost) {
        if (roleId == null || cost <= 0) {
            return;
        }
        try {
            int delta = (int) Math.min(Integer.MAX_VALUE, cost);
            taskProgressPublisher.publish(roleId, "spend_gold", delta, "websocket-shop");
        } catch (Exception e) {
            log.warn("[Shop] Failed to report spend_gold for roleId={}: {}", roleId, e.getMessage());
        }
    }

    /**
     * After a successful purchase, fetch the latest wallet balances for the role
     * and push each currency as a {@code PB_SCKnapsackSingleInfo} so the client
     * can update its local gold / diamond counters immediately.
     */
    private void pushWalletBalance(PlayerSession session, Long roleId) {
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(session, walletResp.balances());
                log.debug("[Shop] Pushed wallet balances for roleId={}, items={}", roleId, walletResp.balances().size());
            }
        } catch (Exception e) {
            // Non-critical: log and continue; the buy already succeeded
            log.warn("[Shop] Failed to push wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }
}
