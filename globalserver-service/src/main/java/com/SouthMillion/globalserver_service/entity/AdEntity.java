package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ad")
@Setter
@Getter
public class AdEntity {
    @Id
    private Long userId;
    private Integer todayCount;      // Số lần đã xem/ngày
    private Long nextFetchTime;      // Lần nhận thưởng tiếp theo (timestamp)
    private Long lastUpdate;         // Cập nhật cuối cùng
    // ...
}