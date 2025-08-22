package com.SouthMillion.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallet_account",
        uniqueConstraints = @UniqueConstraint(name="uk_wallet_role_item", columnNames = {"role_id","item_id"}))
@Getter @Setter @NoArgsConstructor
public class WalletAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false, length=64)
    private String roleId;

    @Column(name="item_id", nullable=false)
    private Long itemId;

    @Column(name="balance", nullable=false)
    private Long balance;

    @Column(name="updated_at", nullable=false)
    private Long updatedAtEpochSec;

    @Version
    private Long ver;
}