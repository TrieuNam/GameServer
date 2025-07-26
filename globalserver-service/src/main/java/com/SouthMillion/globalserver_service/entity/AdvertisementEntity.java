package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.globalserver.AdvertisementDTO;

@Entity
@Table(name = "advertisement")
@Data
public class AdvertisementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Integer seq;
    private Integer todayCount;
    private Integer nextFetchTime;


    public static AdvertisementDTO fromEntity(AdvertisementEntity entity) {
        AdvertisementDTO dto = new AdvertisementDTO();
        dto.setSeq(entity.getSeq());
        dto.setTodayCount(entity.getTodayCount());
        dto.setNextFetchTime(entity.getNextFetchTime());
        return dto;
    }
}