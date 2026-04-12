package com.SouthMillion.rune_service.repository;

import com.SouthMillion.rune_service.model.entity.RuneTowerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RuneTowerStateRepository extends JpaRepository<RuneTowerState, Long> {

    /** Reset daily_reward = 0 cho tất cả player (gọi vào 0h mỗi ngày) */
    @Modifying
    @Query("UPDATE RuneTowerState s SET s.dailyReward = 0")
    void resetAllDailyRewards();

    /** Reset daily_reward = 0 cho 1 player cụ thể */
    @Modifying
    @Query("UPDATE RuneTowerState s SET s.dailyReward = 0 WHERE s.roleId = :roleId")
    void resetDailyReward(@Param("roleId") Long roleId);
}
