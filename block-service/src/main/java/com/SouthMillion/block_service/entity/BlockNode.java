package com.SouthMillion.block_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "block_node", uniqueConstraints = {
    @UniqueConstraint(name = "uk_block_role_map_index", columnNames = {"role_id", "map_id", "block_index"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "block_id", nullable = false)
    private Integer blockId;

    @Column(name = "map_id", nullable = false)
    private Integer mapId;

    @Column(name = "block_index", nullable = false)
    private Integer blockIndex;

    @Column(name = "pos_x", nullable = false)
    private Integer posX;

    @Column(name = "pos_y", nullable = false)
    private Integer posY;

    @Column(name = "color", nullable = false)
    private Integer color;
}
