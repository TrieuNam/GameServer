package com.SouthMillion.wallet_service.repository;

import com.SouthMillion.wallet_service.entity.WalletAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
    Optional<WalletAccount> findByRoleIdAndItemId(Long roleId, Long itemId);
    List<WalletAccount> findByRoleIdAndItemIdIn(Long roleId, Collection<Long> itemIds);
    List<WalletAccount> findByRoleId(Long roleId); // <— dùng cho /info
    @Modifying
    @Query("""
      update WalletAccount a
         set a.balance = a.balance + :delta,
             a.updatedAtEpochSec = :now
       where a.id = :id
         and (a.balance + :delta) >= 0
    """)
    int applyDeltaIfEnough(@Param("id") Long id, @Param("delta") long delta, @Param("now") long now);
}