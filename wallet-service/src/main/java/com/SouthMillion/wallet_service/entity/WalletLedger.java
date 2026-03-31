package com.SouthMillion.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="wallet_ledger",
        indexes = {
                @Index(name="idx_wl_role_time", columnList = "role_id,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name="uk_wl_idem", columnNames = {"idem_key"})
        })
@Getter @Setter @NoArgsConstructor
public class WalletLedger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false)
    private Long roleId;

    @Column(name="item_id", nullable=false)
    private Long itemId;

    @Column(name="delta", nullable=false)
    private Long delta; // +n (add) hoặc -n (cost)

    @Column(name="reason")
    private Integer reason;

    @Column(name="reason_type")
    private Integer reasonType;

    @Column(name="idem_key", length=100)
    private String idemKey;

    @Column(name="created_at", nullable=false)
    private Long createdAtEpochSec;
}