package com.southMillion.equip_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "limit_core")
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LimitCoreEntity {
    @Id
    private Long userId;
    private String coreLevelJson; // json array [int,...]
    // getters/setters
}