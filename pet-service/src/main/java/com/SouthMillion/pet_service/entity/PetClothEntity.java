package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_pet_cloth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetClothEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleId;
    private Integer itemId;
    private Integer level;
    private Integer petIndex;
}