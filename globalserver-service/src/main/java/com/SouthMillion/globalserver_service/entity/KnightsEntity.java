package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.SouthMillion.dto.globalserver.KnightsDTO;

@Entity
@Table(name = "knights")
@Data
public class KnightsEntity {
    @Id
    private Long userId;
    private Integer level;
    private Integer flag;
    private Integer levelFlag;

    public static KnightsDTO fromEntity(KnightsEntity entity) {
        KnightsDTO dto = new KnightsDTO();
        dto.setLevel(entity.getLevel());
        dto.setFlag(entity.getFlag());
        dto.setLevelFlag(entity.getLevelFlag());
        return dto;
    }
}