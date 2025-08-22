package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "player_pet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleId;
    private Integer petIndex;
    private Integer petId;
    private Integer petLevel;
    private Integer petExp;
    private Integer petOrder;
    @ElementCollection
    private List<Integer> skillList;
    @ElementCollection
    private List<Integer> gemItemId;
    @ElementCollection
    private List<Integer> tsGemIndex;
    @ElementCollection
    private List<Integer> attrList;
    private Long capability;
    private Integer skillLockFlag;
}