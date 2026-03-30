package com.SouthMillion.analytics_service.service;

import com.SouthMillion.analytics_service.entity.PlayerKpi;
import com.SouthMillion.analytics_service.repository.PlayerKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Dedicated service for KPI metric updates.
 *
 * Runs in its own REQUIRES_NEW transaction so that a duplicate-key race condition
 * (two concurrent requests inserting the same player+date row) can be caught and
 * retried without rolling back the parent event-save transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiUpdateService {

    private final PlayerKpiRepository kpiRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateKpiMetrics(Long playerId, String eventType, Map<String, Object> eventData) {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);

        PlayerKpi kpi = kpiRepository.findByPlayerIdAndDate(playerId, today)
                .orElseGet(() -> {
                    PlayerKpi newKpi = new PlayerKpi();
                    newKpi.setPlayerId(playerId);
                    newKpi.setDate(today);
                    return newKpi;
                });

        applyKpiUpdate(kpi, eventType, eventData);

        kpiRepository.save(kpi);
    }

    private void applyKpiUpdate(PlayerKpi kpi, String eventType, Map<String, Object> eventData) {
        switch (eventType) {
            case "player.login":
                kpi.setLoginCount(kpi.getLoginCount() + 1);
                break;
            case "player.logout":
                if (eventData.containsKey("duration")) {
                    int duration = ((Number) eventData.get("duration")).intValue();
                    kpi.setSessionDuration(kpi.getSessionDuration() + duration);
                }
                break;
            case "wallet.spent":
            case "shop.purchase":
                if (eventData.containsKey("amount")) {
                    BigDecimal amount = new BigDecimal(eventData.get("amount").toString());
                    kpi.setTotalSpent(kpi.getTotalSpent().add(amount));
                    kpi.setPurchaseCount(kpi.getPurchaseCount() + 1);
                }
                break;
            case "wallet.earned":
                if (eventData.containsKey("amount")) {
                    BigDecimal amount = new BigDecimal(eventData.get("amount").toString());
                    kpi.setTotalEarned(kpi.getTotalEarned().add(amount));
                }
                break;
            case "battle.started":
                kpi.setBattlesPlayed(kpi.getBattlesPlayed() + 1);
                break;
            case "battle.won":
                kpi.setBattlesWon(kpi.getBattlesWon() + 1);
                break;
            case "arena.match":
                kpi.setPvpMatches(kpi.getPvpMatches() + 1);
                break;
            case "task.completed":
                kpi.setTasksCompleted(kpi.getTasksCompleted() + 1);
                break;
            case "role.levelup":
                kpi.setLevelsGained(kpi.getLevelsGained() + 1);
                break;
            case "chat.message":
                kpi.setMessagesent(kpi.getMessagesent() + 1);
                break;
            case "friend.request":
                kpi.setFriendRequests(kpi.getFriendRequests() + 1);
                break;
            default:
                // No KPI metric for this event type
                break;
        }
    }
}

