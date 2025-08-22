package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.BagSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BagSlotRepository extends JpaRepository<BagSlot, Long> {

    @Query("select s from BagSlot s where s.roleId=:roleId and s.bagType=:bagType order by s.slotIndex asc")
    List<BagSlot> findByRoleAndBag(@Param("roleId") String roleId, @Param("bagType") byte bagType);

    @Query("select s from BagSlot s where s.roleId=:roleId and s.bagType=:bagType and s.itemId=:itemId order by s.expireAt asc nulls first, s.count asc")
    List<BagSlot> findStacks(@Param("roleId") String roleId, @Param("bagType") byte bagType, @Param("itemId") int itemId);

    Optional<BagSlot> findByRoleIdAndBagTypeAndSlotIndex(String roleId, byte bagType, int slotIndex);

    @Query("select count(s) from BagSlot s where s.roleId=:roleId and s.bagType=:bagType")
    long countUsed(@Param("roleId") String roleId, @Param("bagType") byte bagType);
}