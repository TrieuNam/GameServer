package com.SouthMillion.guild_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Guild Warehouse Item Entity
 * 
 * Represents items in guild warehouse
 * Max slots: 100
 */
@Entity
@Table(name = "guild_warehouse", indexes = {
    @Index(name = "idx_warehouse_guild", columnList = "guildId"),
    @Index(name = "idx_warehouse_item", columnList = "itemId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildWarehouseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Guild ID
     */
    @Column(nullable = false)
    private Long guildId;

    /**
     * Item ID (from item-service)
     */
    @Column(nullable = false)
    private Integer itemId;

    /**
     * Item name
     */
    @Column(nullable = false, length = 100)
    private String itemName;

    /**
     * Item quantity
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Item quality (1-5)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quality = 1;

    /**
     * Depositor role ID
     */
    @Column(nullable = false)
    private String depositorId;

    /**
     * Depositor name
     */
    @Column(nullable = false, length = 50)
    private String depositorName;

    /**
     * Deposit time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime depositedAt;

    /**
     * Add quantity
     */
    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    /**
     * Remove quantity
     * @return true if all removed
     */
    public boolean removeQuantity(int amount) {
        this.quantity -= amount;
        return this.quantity <= 0;
    }
}
