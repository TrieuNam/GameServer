package com.SouthMillion.wallet_service.repository;

import com.SouthMillion.wallet_service.entity.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    Optional<WalletLedger> findByIdemKey(String idemKey);
}