package com.SouthMillion.bag_service.enity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bag_slot",
        uniqueConstraints = @UniqueConstraint(name="uk_role_bag_slot", columnNames = {"role_id","bag_type","slot_index"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", length=40, nullable=false) private String roleId;
    @Column(name="bag_type", nullable=false) private byte bagType;
    @Column(name="slot_index", nullable=false) private int slotIndex;

    @Column(name="item_id", nullable=false) private int itemId;
    @Column(name="count", nullable=false) private long count;
    @Column(name="bind", nullable=false) private boolean bind;

    @Column(name="expire_at") private LocalDateTime expireAt;

    @Column(name="extra_json", columnDefinition = "json")
    private String extraJson;

    @Version
    private int version;
}