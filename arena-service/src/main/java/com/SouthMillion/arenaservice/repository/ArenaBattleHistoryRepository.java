package com.SouthMillion.arenaservice.repository;

import com.SouthMillion.arenaservice.entity.ArenaBattleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArenaBattleHistoryRepository extends JpaRepository<ArenaBattleHistory, Long> {
    
    @Query("SELECT h FROM ArenaBattleHistory h WHERE h.player1Id = :playerId OR h.player2Id = :playerId ORDER BY h.timestamp DESC")
    Page<ArenaBattleHistory> findPlayerHistory(String playerId, Pageable pageable);
    
    @Query("SELECT h FROM ArenaBattleHistory h WHERE h.player1Id = :playerId OR h.player2Id = :playerId ORDER BY h.timestamp DESC")
    List<ArenaBattleHistory> findRecentBattles(String playerId);
}
