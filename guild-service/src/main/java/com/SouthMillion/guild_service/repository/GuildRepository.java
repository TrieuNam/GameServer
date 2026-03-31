package com.SouthMillion.guild_service.repository;

import com.SouthMillion.guild_service.entity.Guild;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guild Repository
 */
@Repository
public interface GuildRepository extends JpaRepository<Guild, Long> {

    /**
     * Find guild by name
     */
    Optional<Guild> findByName(String name);

    /**
     * Find guild by name (case insensitive)
     */
    Optional<Guild> findByNameIgnoreCase(String name);

    /**
     * Find active guild by name
     */
    Optional<Guild> findByNameAndActiveTrue(String name);

    /**
     * Find guild by leader ID
     */
    Optional<Guild> findByLeaderIdAndActiveTrue(Long leaderId);

    /**
     * Check if guild name exists
     */
    boolean existsByName(String name);

    /**
     * Check if guild name exists (case insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find all active guilds
     */
    List<Guild> findAllByActiveTrueOrderByLevelDescExpDesc();

    /**
     * Find guilds by level range
     */
    List<Guild> findByActiveTrueAndLevelBetweenOrderByLevelDescExpDesc(Integer minLevel, Integer maxLevel);

    /**
     * Search guilds by name pattern
     */
    @Query("SELECT g FROM Guild g WHERE g.active = true AND LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY g.level DESC, g.exp DESC")
    Page<Guild> searchByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find top guilds by level
     */
    @Query("SELECT g FROM Guild g WHERE g.active = true ORDER BY g.level DESC, g.exp DESC, g.memberCount DESC")
    Page<Guild> findTopGuilds(Pageable pageable);

    /**
     * Get guild ranking
     */
    @Query("SELECT COUNT(g) + 1 FROM Guild g WHERE g.active = true AND (g.level > :level OR (g.level = :level AND g.exp > :exp))")
    Long getGuildRank(@Param("level") Integer level, @Param("exp") Long exp);

    /**
     * Count active guilds
     */
    long countByActiveTrue();

    /**
     * Find guilds recruiting (not full)
     */
    @Query("SELECT g FROM Guild g WHERE g.active = true AND g.memberCount < g.maxMembers ORDER BY g.level DESC")
    Page<Guild> findRecruitingGuilds(Pageable pageable);
}
