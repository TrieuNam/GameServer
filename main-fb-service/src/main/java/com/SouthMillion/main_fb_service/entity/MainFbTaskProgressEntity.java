package com.SouthMillion.main_fb_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mainfb_task_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainFbTaskProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=64, unique=true)
    private String playerId;

    /** Đã hoàn thành tới nhiệm vụ số mấy (index 1-based). Null = chưa có tiến độ. */
    private Integer doneIndex;

    @Column(nullable=false)
    private Long updatedAt;

    public void markDone(int index) {
        if (doneIndex == null || index > doneIndex) {
            doneIndex = index;
        }
        updatedAt = Instant.now().getEpochSecond();
    }
}