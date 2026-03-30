package com.SouthMillion.task_service.mapper;

import com.SouthMillion.task_service.entity.model_clothes.PlayerClothesEntity;
import org.SouthMillion.dto.ShiZhuang.PlayerClothesDTO;

public class PlayerClothesMapper {
    public static PlayerClothesDTO toDTO(PlayerClothesEntity entity) {
        if (entity == null) return null;
        return new PlayerClothesDTO(
                entity.getId(),
                entity.getPlayerId() != null ? String.valueOf(entity.getPlayerId()) : null,
                entity.getClothesId(),
                entity.getLevel()
        );
    }

    public static PlayerClothesEntity toEntity(PlayerClothesDTO dto) {
        if (dto == null) return null;
        return PlayerClothesEntity.builder()
                .id(dto.getId())
                .playerId(dto.getPlayerId() != null ? Long.parseLong(dto.getPlayerId()) : null)
                .clothesId(dto.getClothesId())
                .level(dto.getLevel())
                .build();
    }
}