package com.southMillion.equip_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gem")
@Data
@Setter
@Getter
public class GemEntity {
    @Id
    private Long userId;
    private Integer drawingId;
    private String gemListJson; // json array [{itemId, pos}]
    // getters/setters
}
