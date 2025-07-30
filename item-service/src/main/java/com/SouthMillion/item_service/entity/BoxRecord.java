package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "box_record")
@Setter
@Getter
public class BoxRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String boxType;
    private Integer boxId;
    private Integer rewardItemId;
    private Integer rewardNum;
    private Integer rewardType;
    private Integer sellPrice;
    private Integer exp;
    private Integer timestamp;
}