package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "mystery_shop_state")
@Data
public class MysteryShopStateEntity {
    @Id
    private Long userId;

    private Integer buyFlag; // bitmask
    private String indexList; // ví dụ "1,5,8"
}
