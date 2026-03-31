package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.MonthCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthCardRepository extends JpaRepository<MonthCard, Long> {
    List<MonthCard> findByRoleId(Long roleId);
    Optional<MonthCard> findByRoleIdAndCardType(Long roleId, Integer cardType);
}
