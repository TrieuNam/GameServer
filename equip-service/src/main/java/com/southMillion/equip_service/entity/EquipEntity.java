package com.southMillion.equip_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private int equipType;
    private int itemId;
    private int level;
    private int hp;
    private int attack;
    private int defend;
    private int speed;

    private Integer attrType1;   // Thêm nếu cần
    private Integer attrValue1;
    private Integer attrType2;
    private Integer attrValue2;

}