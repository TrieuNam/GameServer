package com.SouthMillion.shop_service.repository;

import com.SouthMillion.shop_service.entity.ShopLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopLimitRepository extends JpaRepository<ShopLimit, Long> {
    Optional<ShopLimit> findByRoleIdAndKindAndEntryIndexAndPeriodAndDayStr(
            Long roleId, String kind, int entryIndex, String period, String dayStr);
}