package com.SouthMillion.shop_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shop_limit",
        uniqueConstraints = @UniqueConstraint(name="uk_shop_limit",
                columnNames={"role_id","kind","entry_index","period","day_str"}))
@Getter @Setter
public class ShopLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false, length=64)
    private String roleId;

    @Column(name="kind", nullable=false, length=16) // COMMON/CLOTH/SHENMI
    private String kind;

    @Column(name="entry_index", nullable=false) // index hoặc seq
    private int entryIndex;

    @Column(name="period", nullable=false, length=16) // DAILY/FOREVER
    private String period;

    @Column(name="day_str", nullable=false, length=16) // yyyyMMdd (DAILY), hoặc "ALL" (FOREVER)
    private String dayStr;

    @Column(name="count", nullable=false)
    private long count;
}