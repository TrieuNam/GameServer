package com.SouthMillion.user_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "limit_core")
@Getter
@Setter
public class LimitCoreEntity {
    @EmbeddedId
    private LimitCoreKey id;

    private Integer level;

    // getter/setter

    @Embeddable
    @Getter
    @Setter
    public static class LimitCoreKey implements Serializable {
        private Long userId;
        private Integer coreIndex;
        // getter/setter, equals/hashCode
    }
}