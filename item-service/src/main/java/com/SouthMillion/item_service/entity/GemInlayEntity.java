package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class GemInlayEntity {
    @Id
    private Long id;
    private Integer itemId;
    private Integer pos;
}