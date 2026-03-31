package com.SouthMillion.activity_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "market_shop",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketShop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;
    @Column(name = "end_timestamp", nullable = false)
    private Long endTimestamp;
    @Column(name = "next_free_refresh", nullable = false)
    private Long nextFreeRefresh;
    @Column(name = "next_auto_refresh", nullable = false)
    private Long nextAutoRefresh;
    @Column(name = "cur_shop_group", nullable = false)
    private Integer curShopGroup;
    /** JSON arrays */
    @Column(name = "shop_goods_seq_json", nullable = false, length = 512)
    private String shopGoodsSeqJson;
    @Column(name = "shop_buy_times_json", nullable = false, length = 512)
    private String shopBuyTimesJson;
    @Column(name = "random_cnts", nullable = false)
    private Integer randomCnts;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
