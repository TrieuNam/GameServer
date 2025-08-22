package com.SouthMillion.task_service.mapper;

import com.SouthMillion.task_service.entity.model_clothes.PlayerClothesEntity;

public class PlayerClothesMapper {
    public static PlayerClothesDTO toDTO(PlayerClothesEntity entity) {
        if (entity == null) return null;
        return new PlayerClothesDTO(
                entity.getId(),
                entity.getPlayerId(),
                entity.getClothesId(),
                entity.getLevel()
        );
    }

    public static PlayerClothesEntity toEntity(PlayerClothesDTO dto) {
        if (dto == null) return null;
        return PlayerClothesEntity.builder()
                .id(dto.getId())
                .playerId(dto.getPlayerId())
                .clothesId(dto.getClothesId())
                .level(dto.getLevel())
                .build();
    }
}