package com.SouthMillion.bag_service.enity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bag_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagMeta {

    @EmbeddedId
    private BagMetaId id;

    @Column(nullable = false)
    private int capacity;

    @Version
    private int version;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class BagMetaId {
        @Column(name = "role_id", length = 40)
        private String roleId;
        @Column(name = "bag_type")
        private byte bagType;
    }
}