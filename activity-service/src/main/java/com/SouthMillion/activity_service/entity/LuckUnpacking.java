package com.SouthMillion.activity_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "luck_unpacking",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LuckUnpacking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;
    @Column(name = "receive_flag", nullable = false)
    private Integer receiveFlag;
    @Column(name = "open_box_num", nullable = false)
    private Integer openBoxNum;
    @Column(name = "box_level", nullable = false)
    private Integer boxLevel;
    @Column(name = "end_timestamp", nullable = false)
    private Long endTimestamp;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
