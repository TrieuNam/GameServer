package com.SouthMillion.guild_service.repository;

import com.SouthMillion.guild_service.entity.GuildApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Guild Application Repository
 */
@Repository
public interface GuildApplicationRepository extends JpaRepository<GuildApplication, Long> {

    /**
     * Find application by guild and role
     */
    Optional<GuildApplication> findByGuildIdAndRoleId(Long guildId, Long roleId);

    /**
     * Find pending application by guild and role
     */
    Optional<GuildApplication> findByGuildIdAndRoleIdAndStatus(Long guildId, Long roleId, Integer status);

    /**
     * Find player's applications
     */
    List<GuildApplication> findByRoleIdOrderByAppliedAtDesc(Long roleId);

    /**
     * Find pending applications for guild
     */
    List<GuildApplication> findByGuildIdAndStatusOrderByAppliedAtDesc(Long guildId, Integer status);

    /**
     * Find all applications for guild
     */
    List<GuildApplication> findByGuildIdOrderByStatusAscAppliedAtDesc(Long guildId);

    /**
     * Count pending applications for guild
     */
    long countByGuildIdAndStatus(Long guildId, Integer status);

    /**
     * Check if player has pending application to guild
     */
    boolean existsByGuildIdAndRoleIdAndStatus(Long guildId, Long roleId, Integer status);

    /**
     * Delete old processed applications
     */
    @Modifying
    @Query("DELETE FROM GuildApplication a WHERE a.status != 0 AND a.processedAt < :cutoffTime")
    void deleteOldProcessedApplications(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete all applications for guild
     */
    @Modifying
    @Query("DELETE FROM GuildApplication a WHERE a.guildId = :guildId")
    void deleteByGuildId(@Param("guildId") Long guildId);

    /**
     * Delete player's applications
     */
    @Modifying
    @Query("DELETE FROM GuildApplication a WHERE a.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
