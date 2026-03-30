package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 41: 个性化礼包 (Customized Gift)
 * Personalized gift pack with multiple fetch flags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customized_gift")
public class CustomizedGift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "role_level", nullable = false)
    private Integer roleLevel; // Player level

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen; // Is gift shop open

    @Column(name = "has_buy_gift", nullable = false)
    private Boolean hasBuyGift; // Has purchased any gift

    @Column(name = "fetch_flags_json", nullable = false, columnDefinition = "TEXT")
    private String fetchFlagsJson; // JSON array of fetch flag enum values

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
