package com.SouthMillion.scroll_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
/** Individual scroll item owned by a role */
@Entity @Table(name = "scroll_item",
    indexes = @Index(name = "idx_role_id", columnList = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrollItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    @Column(name = "scroll_index", nullable = false)
    private Integer scrollIndex;
    @Column(name = "item_id", nullable = false)
    private Integer itemId;
    @Column(name = "level", nullable = false)
    private Integer level;
    @Column(name = "wear_mark", nullable = false)
    private Integer wearMark;
    @Column(name = "param", nullable = false)
    private Integer param;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
