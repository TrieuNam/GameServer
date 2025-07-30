package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class GemDrawingEntity {
    @Id
    private Long id;
    private Long userId;
    private Integer level;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "drawing_id")
    private List<GemInlayEntity> gemList;
}
