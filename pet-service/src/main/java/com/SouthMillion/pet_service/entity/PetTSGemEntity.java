package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "player_pet_ts_gem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetTSGemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleId;
    private Integer gemIndex;
    private Integer gemLevel;
    private Integer petIndex;
    @ElementCollection
    private List<Integer> attrType;
    @ElementCollection
    private List<Integer> attrValue;
}