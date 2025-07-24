package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_recycle")
@Setter
@Getter
public class ItemRecycleEntity {
    @Id
    private Long userId;
    private Integer level;
    private Long exp;
    // getters/setters
}