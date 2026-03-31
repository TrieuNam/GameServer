package com.SouthMillion.guild_service.repository;

import com.SouthMillion.guild_service.entity.GuildMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Guild Member Repository
 */
@Repository
public interface GuildMemberRepository extends JpaRepository<GuildMember, Long> {

    /**
     * Find member by guild and role
     */
    Optional<GuildMember> findByGuildIdAndRoleId(Long guildId, Long roleId);

    /**
     * Find member by role ID
     */
    Optional<GuildMember> findByRoleId(Long roleId);

    /**
     * Check if player is in guild
     */
    boolean existsByRoleId(Long roleId);

    /**
     * Find all members of a guild
     */
    List<GuildMember> findByGuildIdOrderByRankDescContributionDesc(Long guildId);

    /**
     * Find guild leader
     */
    Optional<GuildMember> findByGuildIdAndRank(Long guildId, Integer rank);

    /**
     * Find officers and leader
     */
    List<GuildMember> findByGuildIdAndRankGreaterThanEqualOrderByRankDesc(Long guildId, Integer minRank);

    /**
     * Find online members
     */
    List<GuildMember> findByGuildIdAndOnlineTrue(Long guildId);

    /**
     * Count guild members
     */
    long countByGuildId(Long guildId);

    /**
     * Count online members
     */
    long countByGuildIdAndOnlineTrue(Long guildId);

    /**
     * Delete all members of guild
     */
    @Modifying
    @Query("DELETE FROM GuildMember m WHERE m.guildId = :guildId")
    void deleteByGuildId(@Param("guildId") Long guildId);

    /**
     * Reset daily donations for all members
     */
    @Modifying
    @Query("UPDATE GuildMember m SET m.dailyDonationCount = 0 WHERE m.lastDonationTime < :resetTime")
    void resetDailyDonations(@Param("resetTime") LocalDateTime resetTime);

    /**
     * Find top contributors in guild
     */
    @Query("SELECT m FROM GuildMember m WHERE m.guildId = :guildId ORDER BY m.contribution DESC")
    List<GuildMember> findTopContributors(@Param("guildId") Long guildId);

    /**
     * Update member power
     */
    @Modifying
    @Query("UPDATE GuildMember m SET m.power = :power WHERE m.roleId = :roleId")
    void updateMemberPower(@Param("roleId") Long roleId, @Param("power") Long power);

    /**
     * Update member level
     */
    @Modifying
    @Query("UPDATE GuildMember m SET m.roleLevel = :level WHERE m.roleId = :roleId")
    void updateMemberLevel(@Param("roleId") Long roleId, @Param("level") Integer level);
}
