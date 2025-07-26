package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.item.MysteryShopDTO;

import static com.SouthMillion.item_service.service.MysteryShopService.parseIndexes;

@Entity
@Table(name = "mystery_shop")
@Data
public class MysteryShopEntity {
    @Id
    private Integer userId;
    private Integer buyFlag;
    private String indexListJson;

    public static MysteryShopDTO fromEntity(MysteryShopEntity entity) {
        MysteryShopDTO dto = new MysteryShopDTO();
        dto.setBuyFlag(entity.getBuyFlag());
        dto.setIndexList(parseIndexes(entity.getIndexListJson()));
        return dto;
    }
}