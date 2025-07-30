package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_attr_detail2")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAttrDetail2Entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_attr_detail_id")
    private Integer id;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "monitor_data")
    private byte[] monitorData;

    @Column(name = "common_data")
    private byte[] commonData;

    @Column(name = "system_set_data")
    private byte[] systemSetData;

    @Column(name = "role_cross_data")
    private byte[] roleCrossData;

    @Column(name = "ling_zhu_data")
    private byte[] lingZhuData;

    @Column(name = "equip_data")
    private byte[] equipData;

    @Column(name = "box_data")
    private byte[] boxData;

    @Column(name = "shi_lian_pagoda_data")
    private byte[] shiLianPagodaData;

    @Column(name = "gu_mo_pagoda_data")
    private byte[] guMoPagodaData;

    @Column(name = "knights_data")
    private byte[] knightsData;

    @Column(name = "angel_data")
    private byte[] angelData;

    @Column(name = "mystery_shop_data")
    private byte[] mysteryShopData;

    @Column(name = "mount_data")
    private byte[] mountData;

    @Column(name = "star_map_data")
    private byte[] starMapData;

    @Column(name = "role_data_record")
    private byte[] roleDataRecord;
}