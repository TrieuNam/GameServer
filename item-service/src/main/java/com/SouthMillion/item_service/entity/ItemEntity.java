package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "user_item")
@Data
public class ItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Integer itemId;
    private Integer count;

    public static ItemDTO fromEntity(ItemEntity e) {
        ItemDTO dto = new ItemDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
