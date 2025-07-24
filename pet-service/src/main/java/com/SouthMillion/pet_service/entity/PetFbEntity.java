package com.SouthMillion.pet_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pet_fb")
@Setter
@Getter
public class PetFbEntity {
    @Id
    private Long userId;
    private Integer passLevel;
    private Long fetchFlag;
    // getters/setters
}