package com.SouthMillion.item_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.SouthMillion.dto.item.GemDrawingDTO;

import static org.SouthMillion.utils.JsonUtil.parseGemList;

@Entity
@Table(name = "gem_drawing")
@Data
public class GemDrawingEntity {
    @Id
    private Long id;
    private Long userId;
    private Integer level;
    private String gemListJson;

    public static GemDrawingDTO fromEntity(GemDrawingEntity entity) {
        GemDrawingDTO dto = new GemDrawingDTO();
        dto.setLevel(entity.getLevel());
        dto.setGemList(parseGemList(entity.getGemListJson()));
        return dto;
    }
}