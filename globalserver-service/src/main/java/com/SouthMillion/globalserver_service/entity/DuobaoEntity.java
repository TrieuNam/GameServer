package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.globalserver.DuobaoDTO;

@Entity
@Table(name = "duobao")
@Data
public class DuobaoEntity {
    @Id
    private Long userId;
    private Integer integral;
    private Integer fetchFlag;
    private Integer freeRefreshNum;
    private Long freeRefreshTime;


    public static DuobaoDTO fromEntity(DuobaoEntity entity) {
        DuobaoDTO dto = new DuobaoDTO();
        dto.setIntegral(entity.getIntegral());
        dto.setFetchFlag(entity.getFetchFlag());
        dto.setFreeRefreshNum(entity.getFreeRefreshNum());
        dto.setFreeRefreshTime(entity.getFreeRefreshTime());
        return dto;
    }
}