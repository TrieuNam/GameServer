package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shop_purchase")
@Data
public class ShopPurchaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Integer shopIndex;
    private Integer buyNum;
    private Long lastBuyTime;
}