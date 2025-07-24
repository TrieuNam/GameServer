package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shop_limit")
@Setter
@Getter
public class ShopEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long userId;
    private Integer itemIndex;
    private Integer buyNum;
    // getters/setters
}
