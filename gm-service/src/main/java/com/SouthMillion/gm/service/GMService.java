package com.SouthMillion.gm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.gm.dto.*;
import com.SouthMillion.gm.entity.GMActionLog;
import com.SouthMillion.gm.feign.*;
import com.SouthMillion.gm.repository.GMActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GMService {

    private final GMActionLogRepository actionLogRepository;
    private final BagFeignClient bagFeignClient;
    private final WalletFeignClient walletFeignClient;
    private final RoleFeignClient roleFeignClient;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;

    private static final String BROADCAST_TOPIC = "gm.broadcast";

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    // ==================== Item Management ====================
    
    @Transactional
    public Map<String, Object> giveItems(Long gmId, String gmUsername, GiveItemRequest request, String ipAddress) {
        try {
            log.info("GM {} giving item {} x{} to player {}", gmUsername, request.getItemId(), 
                    request.getQuantity(), request.getPlayerId());
            
            Map<String, Object> bagRequest = Map.of(
                    "itemId", request.getItemId(),
                    "quantity", request.getQuantity()
            );
            Map<String, Object> result = bagFeignClient.addItems(request.getPlayerId(), bagRequest);

            logAction(gmId, gmUsername, "GIVE_ITEM", request.getPlayerId(), null,
                    Map.of("itemId", request.getItemId(), "quantity", request.getQuantity()),
                    request.getReason(), ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to give items: {}", e.getMessage());
            logAction(gmId, gmUsername, "GIVE_ITEM", request.getPlayerId(), null,
                    Map.of("itemId", request.getItemId(), "quantity", request.getQuantity()),
                    request.getReason(), ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to give items: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> removeItems(Long gmId, String gmUsername, GiveItemRequest request, String ipAddress) {
        try {
            log.info("GM {} removing item {} x{} from player {}", gmUsername, request.getItemId(), 
                    request.getQuantity(), request.getPlayerId());
            
            Map<String, Object> bagRequest = Map.of(
                    "itemId", request.getItemId(),
                    "quantity", request.getQuantity()
            );
            Map<String, Object> result = bagFeignClient.removeItems(request.getPlayerId(), bagRequest);

            logAction(gmId, gmUsername, "REMOVE_ITEM", request.getPlayerId(), null,
                    Map.of("itemId", request.getItemId(), "quantity", request.getQuantity()),
                    request.getReason(), ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to remove items: {}", e.getMessage());
            logAction(gmId, gmUsername, "REMOVE_ITEM", request.getPlayerId(), null,
                    Map.of("itemId", request.getItemId(), "quantity", request.getQuantity()),
                    request.getReason(), ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to remove items: " + e.getMessage());
        }
    }

    // ==================== Currency Management ====================
    
    @Transactional
    public Map<String, Object> addCurrency(Long gmId, String gmUsername, CurrencyOperationRequest request, String ipAddress) {
        try {
            log.info("GM {} adding {} {} to player {}", gmUsername, request.getAmount(), 
                    request.getCurrencyType(), request.getPlayerId());
            
            Map<String, Object> walletRequest = Map.of(
                    "currencyType", request.getCurrencyType(),
                    "amount", request.getAmount()
            );
            Map<String, Object> result = walletFeignClient.addCurrency(request.getPlayerId(), walletRequest);

            logAction(gmId, gmUsername, "ADD_CURRENCY", request.getPlayerId(), null,
                    Map.of("currencyType", request.getCurrencyType(), "amount", request.getAmount()),
                    request.getReason(), ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to add currency: {}", e.getMessage());
            logAction(gmId, gmUsername, "ADD_CURRENCY", request.getPlayerId(), null,
                    Map.of("currencyType", request.getCurrencyType(), "amount", request.getAmount()),
                    request.getReason(), ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to add currency: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> deductCurrency(Long gmId, String gmUsername, CurrencyOperationRequest request, String ipAddress) {
        try {
            log.info("GM {} deducting {} {} from player {}", gmUsername, request.getAmount(), 
                    request.getCurrencyType(), request.getPlayerId());
            
            Map<String, Object> walletRequest = Map.of(
                    "currencyType", request.getCurrencyType(),
                    "amount", request.getAmount()
            );
            Map<String, Object> result = walletFeignClient.deductCurrency(request.getPlayerId(), walletRequest);

            logAction(gmId, gmUsername, "DEDUCT_CURRENCY", request.getPlayerId(), null,
                    Map.of("currencyType", request.getCurrencyType(), "amount", request.getAmount()),
                    request.getReason(), ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to deduct currency: {}", e.getMessage());
            logAction(gmId, gmUsername, "DEDUCT_CURRENCY", request.getPlayerId(), null,
                    Map.of("currencyType", request.getCurrencyType(), "amount", request.getAmount()),
                    request.getReason(), ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to deduct currency: " + e.getMessage());
        }
    }

    // ==================== Player Info ====================
    
    public Map<String, Object> getPlayerDetails(String playerId) {
        try {
            Map<String, Object> inventory = bagFeignClient.getInventory(playerId);
            Map<String, Object> wallet = walletFeignClient.getWallet(playerId);
            
            return Map.of(
                    "playerId", playerId,
                    "inventory", inventory,
                    "wallet", wallet
            );
        } catch (Exception e) {
            log.error("Failed to get player details: {}", e.getMessage());
            throw new RuntimeException("Failed to get player details: " + e.getMessage());
        }
    }

    public Map<String, Object> getUserInfo(Long userId) {
        return userFeignClient.getUserInfo(userId);
    }

    public Map<String, Object> getUserRoles(Long userId) {
        List<Map<String, Object>> rolesList = roleFeignClient.getRolesByUserId(String.valueOf(userId));
        return Map.of("roles", rolesList, "count", rolesList != null ? rolesList.size() : 0);
    }

    // ==================== VIP Management ====================
    
    @Transactional
    public Map<String, Object> updateVipLevel(Long gmId, String gmUsername, UpdateVipRequest request, String ipAddress) {
        try {
            log.info("GM {} updating VIP level to {} for user {}", gmUsername, request.getVipLevel(), request.getUserId());
            
            List<Map<String, Object>> rolesList = roleFeignClient.getRolesByUserId(String.valueOf(request.getUserId()));
            if (rolesList == null || rolesList.isEmpty()) {
                throw new RuntimeException("No roles found for userId: " + request.getUserId());
            }
            // roleId field in RoleResp is a String (backward compat)
            Object rawRoleId = rolesList.get(0).get("roleId");
            Long roleId = Long.parseLong(String.valueOf(rawRoleId));
            
            Map<String, Integer> vipRequest = Map.of("vipLevel", request.getVipLevel());
            Map<String, Object> result = roleFeignClient.updateVipLevel(roleId, vipRequest);

            logAction(gmId, gmUsername, "UPDATE_VIP", null, request.getUserId(),
                    Map.of("vipLevel", request.getVipLevel()),
                    request.getReason(), ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to update VIP level: {}", e.getMessage());
            logAction(gmId, gmUsername, "UPDATE_VIP", null, request.getUserId(),
                    Map.of("vipLevel", request.getVipLevel()),
                    request.getReason(), ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to update VIP level: " + e.getMessage());
        }
    }

    // ==================== User Ban/Unban ====================
    
    @Transactional
    public Map<String, Object> banUser(Long gmId, String gmUsername, Long userId, String reason, 
                                       Integer durationDays, String ipAddress) {
        try {
            log.info("GM {} banning user {} for {} days", gmUsername, userId, durationDays);
            
            Map<String, Object> banRequest = Map.of(
                    "reason", reason,
                    "durationDays", durationDays != null ? durationDays : 0
            );
            Map<String, Object> result = userFeignClient.banUser(userId, banRequest);

            logAction(gmId, gmUsername, "BAN_USER", null, userId,
                    Map.of("durationDays", durationDays != null ? durationDays : "permanent"),
                    reason, ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to ban user: {}", e.getMessage());
            logAction(gmId, gmUsername, "BAN_USER", null, userId,
                    Map.of("durationDays", durationDays != null ? durationDays : "permanent"),
                    reason, ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to ban user: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> unbanUser(Long gmId, String gmUsername, Long userId, String reason, String ipAddress) {
        try {
            log.info("GM {} unbanning user {}", gmUsername, userId);
            
            Map<String, Object> result = userFeignClient.unbanUser(userId);

            logAction(gmId, gmUsername, "UNBAN_USER", null, userId,
                    Map.of(), reason, ipAddress, "SUCCESS", null);

            return result;
        } catch (Exception e) {
            log.error("Failed to unban user: {}", e.getMessage());
            logAction(gmId, gmUsername, "UNBAN_USER", null, userId,
                    Map.of(), reason, ipAddress, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to unban user: " + e.getMessage());
        }
    }

    // ==================== Broadcast ====================
    
    @Transactional
    public String broadcastMessage(Long gmId, String gmUsername, BroadcastRequest request, String ipAddress) {
        logAction(gmId, gmUsername, "BROADCAST_MESSAGE", null, null,
                Map.of("message", request.getMessage(), "type", request.getType(), 
                       "duration", request.getDurationSeconds()),
                "System broadcast", ipAddress, "SUCCESS", null);

        log.info("Broadcasting message: {} (type: {}, duration: {}s)", 
                request.getMessage(), request.getType(), request.getDurationSeconds());

        // Publish to Kafka so webSocket-server can push to all connected clients
        if (kafkaTemplate != null) {
            try {
                String payload = objectMapper.writeValueAsString(Map.of(
                        "message", request.getMessage(),
                        "type", request.getType(),
                        "durationSeconds", request.getDurationSeconds()
                ));
                kafkaTemplate.send(BROADCAST_TOPIC, payload);
                log.info("Broadcast message sent to Kafka topic '{}'", BROADCAST_TOPIC);
            } catch (Exception e) {
                log.error("Failed to publish broadcast to Kafka: {}", e.getMessage());
            }
        } else {
            log.warn("KafkaTemplate not available — broadcast message not sent to topic '{}'", BROADCAST_TOPIC);
        }

        return "Message broadcast successfully";
    }

    // ==================== Logs ====================
    
    public Page<GMActionLog> getGMLogs(Long gmId, Pageable pageable) {
        return actionLogRepository.findByGmIdOrderByTimestampDesc(gmId, pageable);
    }

    public Page<GMActionLog> getPlayerActionLogs(String playerId, Pageable pageable) {
        return actionLogRepository.findByTargetPlayerIdOrderByTimestampDesc(playerId, pageable);
    }

    public List<GMActionLog> getRecentLogs() {
        return actionLogRepository.findTop100ByOrderByTimestampDesc();
    }

    // ==================== Private Helper ====================
    
    private void logAction(Long gmId, String gmUsername, String action, String targetPlayerId, 
                          Long targetUserId, Map<String, Object> details, String reason, 
                          String ipAddress, String status, String errorMessage) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            GMActionLog log = GMActionLog.builder()
                    .gmId(gmId)
                    .gmUsername(gmUsername)
                    .action(action)
                    .targetPlayerId(targetPlayerId)
                    .targetUserId(targetUserId)
                    .details(detailsJson)
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            actionLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to log GM action: {}", e.getMessage());
        }
    }
}
