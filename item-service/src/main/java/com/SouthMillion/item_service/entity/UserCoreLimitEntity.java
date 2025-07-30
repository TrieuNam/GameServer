package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_core_limit")
@Data
public class UserCoreLimitEntity {
    @Id
    private Long userId;

    private Integer currentLevel;
    private Integer currentCoreCount;
}
