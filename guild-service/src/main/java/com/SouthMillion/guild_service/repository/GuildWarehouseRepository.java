package com.SouthMillion.guild_service.repository;

import com.SouthMillion.guild_service.entity.GuildWarehouseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guild Warehouse Repository
 */
@Repository
public interface GuildWarehouseRepository extends JpaRepository<GuildWarehouseItem, Long> {

    /**
     * Find all items in guild warehouse
     */
    List<GuildWarehouseItem> findByGuildIdOrderByQualityDescDepositedAtDesc(Long guildId);

    /**
     * Find specific item in warehouse
     */
    Optional<GuildWarehouseItem> findByGuildIdAndItemId(Long guildId, Integer itemId);

    /**
     * Count items in warehouse
     */
    long countByGuildId(Long guildId);

    /**
     * Delete all items from guild warehouse
     */
    @Modifying
    @Query("DELETE FROM GuildWarehouseItem w WHERE w.guildId = :guildId")
    void deleteByGuildId(@Param("guildId") Long guildId);

    /**
     * Find items by depositor
     */
    List<GuildWarehouseItem> findByGuildIdAndDepositorIdOrderByDepositedAtDesc(Long guildId, String depositorId);

    /**
     * Get total item count
     */
    @Query("SELECT COALESCE(SUM(w.quantity), 0) FROM GuildWarehouseItem w WHERE w.guildId = :guildId")
    Long getTotalItemCount(@Param("guildId") Long guildId);
}
