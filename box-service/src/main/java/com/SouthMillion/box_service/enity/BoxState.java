package com.SouthMillion.box_service.enity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="box_state")
@Getter @Setter @NoArgsConstructor
public class BoxState {
    @Id @Column(length=64) private String roleId;

    private int boxLevel = 1;
    private int boxBuyTimes = 0;
    private long levelUpEndEpoch = 0L;
    private int levelFetchFlag = 0;

    private int openBoxTotal = 0;
    private boolean lastOpenIsFive = false;

    @Lob @Column(columnDefinition="TEXT")
    private String pendingJson; // JSON của “pendingOpenedEquip” (color, level, attrs,...)
}