package com.SouthMillion.gameworld_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.SouthMillion.dto.gameworld.MainFbDTO;

@Entity
@Table(name = "main_fb")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MainFbEntity {
    @Id
    private Long userId;
    private Integer level = 0;
    private Integer stage = 0;
    private Integer diaFetchNum = 0;
    private Long lastFetchTime = 0L;

    public static MainFbDTO fromEntity(MainFbEntity entity) {
        return new MainFbDTO(
                entity.getUserId(),
                entity.getLevel(),
                entity.getStage(),
                entity.getDiaFetchNum(),
                entity.getLastFetchTime()
        );
    }
}
