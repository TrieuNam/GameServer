package com.southMillion.report_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private Integer type;     // 0: System Notice, 1: Item Not Enough, 2: SystemMsg, 3: ZeroHour, 4: RechargeRet

    @Column(name = "content", length = 1024)
    private String content;

    @Column(name = "code")
    private Integer code;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "created_time")
    private Long createdTime;
}