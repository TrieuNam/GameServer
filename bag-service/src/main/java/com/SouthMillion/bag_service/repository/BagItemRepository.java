package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.BagItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BagItemRepository extends JpaRepository<BagItem, String> {

    @Query("select b from BagItem b where b.roleId = :roleId")
    List<BagItem> findAllByRoleId(@Param("roleId") String roleId);

    @Query("select b from BagItem b where b.roleId=:roleId and b.itemId=:itemId and b.bind=:bind and ((:exp is null and b.expireAt is null) or b.expireAt=:exp)")
    Optional<BagItem> findExact(@Param("roleId") String roleId,
                                @Param("itemId") Integer itemId,
                                @Param("bind") Boolean bind,
                                @Param("exp") Instant expireAt);

    @Modifying
    @Query("update BagItem b set b.num = b.num + :delta where b.id = :id")
    int incById(@Param("id") String id, @Param("delta") int delta);

    @Modifying
    @Query("update BagItem b set b.num = b.num - :use where b.roleId=:roleId and b.itemId=:itemId and b.num >= :use")
    int consume(@Param("roleId") String roleId, @Param("itemId") Integer itemId, @Param("use") int use);

    @Modifying
    @Query("delete from BagItem b where b.roleId=:roleId and b.itemId=:itemId and b.num<=0")
    int cleanupZero(@Param("roleId") String roleId, @Param("itemId") Integer itemId);
}
