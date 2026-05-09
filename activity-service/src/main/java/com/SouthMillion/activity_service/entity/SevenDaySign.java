package com.SouthMillion.activity_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "seven_day_sign",
uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SevenDaySign {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
@Column(name = "role_id", nullable = false, unique = true)
private Long roleId;
/** Current day count (1-7) */
@Column(name = "days", nullable = false)
private Integer days;
/** Bitmask of claimed days */
@Column(name = "receive_flag", nullable = false)
private Integer receiveFlag;
/** Activity end timestamp (unix seconds) */
@Column(name = "end_timestamp", nullable = false)
private Long endTimestamp;
/** Reward claimed status for each stage */
@Column(name = "has_claimed_reward", nullable = false)
private Boolean hasClaimedReward; // New field for tracking reward claim status
@UpdateTimestamp @Column(name = "updated_at")
private LocalDateTime updatedAt;
}