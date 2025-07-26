package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.item.ItemRecycleDTO;

@Entity
@Table(name = "item_recycle")
@Data
public class ItemRecycleEntity {
    @Id
    private Long userId;
    private Integer level;
    private Long exp;

    public static ItemRecycleDTO fromEntity(ItemRecycleEntity entity) {
        ItemRecycleDTO dto = new ItemRecycleDTO();
        dto.setLevel(entity.getLevel());
        dto.setExp(entity.getExp());
        return dto;
    }
}