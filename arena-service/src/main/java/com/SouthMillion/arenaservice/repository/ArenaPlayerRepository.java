package com.SouthMillion.arenaservice.repository;

import com.SouthMillion.arenaservice.entity.ArenaPlayer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaPlayerRepository extends JpaRepository<ArenaPlayer, String> {
    
    Optional<ArenaPlayer> findByPlayerId(String playerId);
    
    List<ArenaPlayer> findTop100BySeasonOrderByRatingDesc(Integer season);
    
    Page<ArenaPlayer> findBySeasonOrderByRatingDesc(Integer season, Pageable pageable);
    
    @Query("SELECT COUNT(a) + 1 FROM ArenaPlayer a WHERE a.rating > :rating AND a.season = :season")
    Integer calculateRank(Integer rating, Integer season);
    
    @Query("SELECT a FROM ArenaPlayer a WHERE a.season = :season AND a.rating BETWEEN :minRating AND :maxRating ORDER BY RAND() LIMIT 1")
    Optional<ArenaPlayer> findRandomOpponent(Integer season, Integer minRating, Integer maxRating);

    /**
     * Return up to {@code count} distinct random opponents within rating band,
     * excluding the requesting player.
     */
    @Query("SELECT a FROM ArenaPlayer a WHERE a.season = :season AND a.rating BETWEEN :minRating AND :maxRating AND a.playerId <> :excludeId ORDER BY RAND() LIMIT :count")
    List<ArenaPlayer> findRandomOpponents(Integer season, Integer minRating, Integer maxRating, String excludeId, int count);
}
