package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "duobao_info")
@Setter
@Getter
public class DuobaoEntity {
    @Id
    private Long userId;
    private Integer integral;
    private Integer fetchFlag;
    private Integer freeRefreshNum;
    private Long freeRefreshTime;

    // getters/setters
}