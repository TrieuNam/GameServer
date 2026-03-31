package com.SouthMillion.leaderboard_service.repository;

import com.SouthMillion.leaderboard_service.entity.RankingEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RankingRepository extends JpaRepository<RankingEntry, Long> {

    Optional<RankingEntry> findByRankingTypeAndRoleId(Integer rankingType, String roleId);

    List<RankingEntry> findByRankingTypeOrderByScoreDescUpdatedAtAsc(Integer rankingType, Pageable pageable);

    @Query("SELECT r FROM RankingEntry r WHERE r.rankingType = :type ORDER BY r.score DESC, r.updatedAt ASC")
    List<RankingEntry> findTopByType(@Param("type") Integer type, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RankingEntry r WHERE r.rankingType = :type AND r.score > :score")
    Integer getRankByScore(@Param("type") Integer type, @Param("score") Long score);

    @Query("SELECT r FROM RankingEntry r WHERE r.rankingType = :type AND r.roleId = :roleId")
    Optional<RankingEntry> findPlayerRank(@Param("type") Integer type, @Param("roleId") String roleId);
}
