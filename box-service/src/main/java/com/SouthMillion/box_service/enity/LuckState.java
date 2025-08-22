package com.SouthMillion.box_service.enity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="luck_state")
@Getter @Setter @NoArgsConstructor
public class LuckState {
    @Id @Column(length=64) private String roleId;
    private long startEpoch = 0L;
    private long endEpoch   = 0L;
    private long receiveBitmap = 0L;
    private int  snapshotOpenCnt = 0;
}