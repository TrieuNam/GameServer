package com.SouthMillion.bag_service.enity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Entity
@Table(name = "bag_items",
        indexes = {
                @Index(name = "idx_bag_items_role_id", columnList = "role_id"),
                @Index(name = "idx_bag_items_user_id", columnList = "user_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BagItem extends Auditable  implements org.springframework.data.domain.Persistable<String> {

    @Id
    @Column(length = 36)
    private String id; // UUID

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "num", nullable = false)
    private Long num;

    @Column(name = "bind", nullable = false)
    private Boolean bind;

    // Cho phép null nếu item không có hạn
    @Column(name = "expire_at")
    private Instant expireAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Override public String getId() { return id; }
    @Override public boolean isNew() { return getVersion() == null; } // mới khi version null
}