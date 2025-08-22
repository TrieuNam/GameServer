package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PlayerMountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerMountRepository extends JpaRepository<PlayerMountEntity, Long> {

    /** Tìm mount của player theo mountId */
    Optional<PlayerMountEntity> findByPlayerIdAndMountId(String playerId, Integer mountId);

    /** Lấy toàn bộ danh sách mount của player */
    List<PlayerMountEntity> findByPlayerId(String playerId);
}