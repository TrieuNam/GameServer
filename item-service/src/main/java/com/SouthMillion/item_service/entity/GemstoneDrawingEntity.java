package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "gemstone_drawing")
@Data
public class GemstoneDrawingEntity {
    @Id
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer param;
    private Integer getTheSource;
}
