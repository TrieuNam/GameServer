package com.SouthMillion.activity_service.repository;
import com.SouthMillion.activity_service.entity.MarketShop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MarketShopRepository extends JpaRepository<MarketShop, Long> {
    Optional<MarketShop> findByRoleId(Long roleId);
}
