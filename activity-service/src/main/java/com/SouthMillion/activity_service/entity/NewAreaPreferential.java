package com.SouthMillion.activity_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "new_area_preferential",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewAreaPreferential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;
    /** JSON array of buy counts for 12 items */
    @Column(name = "buy_times_json", nullable = false, length = 512)
    private String buyTimesJson;
    @Column(name = "end_timestamp", nullable = false)
    private Long endTimestamp;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
