package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_attr_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAttrDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_attr_detail_id")
    private Integer id;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "history_recharge_gold")
    private Long historyRechargeGold;

    @Column(name = "forbid_talk_time")
    private Long forbidTalkTime;

    @Column(name = "exp")
    private Long exp;

    @Column(name = "max_exp")
    private Long maxExp;

    @Column(name = "capability")
    private Integer capability;

    @Column(name = "max_capability")
    private Integer maxCapability;

    @Column(name = "avatar_timestamp")
    private Long avatarTimestamp;

    @Column(name = "today_online_time")
    private Integer todayOnlineTime;

    @Column(name = "lastday_online_time")
    private Integer lastdayOnlineTime;

    @Column(name = "authority_type")
    private Byte authorityType;

    @Column(name = "battle_data")
    private byte[] battleData; // dùng byte[] cho varbinary

    @Column(name = "appearance_data")
    private byte[] appearanceData;

    // ... các trường khác nếu cần
}