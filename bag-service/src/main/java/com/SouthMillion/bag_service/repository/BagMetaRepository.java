package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.BagMeta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BagMetaRepository extends JpaRepository<BagMeta, String> {

    Optional<BagMeta> findByRoleIdAndBagType(String roleId, byte bagType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from BagMeta m where m.roleId = :rid and m.bagType = :bagType")
    Optional<BagMeta> lockByRoleAndType(@Param("rid") String rid, @Param("bagType") byte bagType);

    @Modifying
    @Query(value = """
            INSERT INTO bag_meta (id, role_id, bag_type, capacity, used, created_at, updated_at)
            VALUES (:id, :rid, :bagType, :capacity, 0, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    void upsert(@Param("id") String id,
                @Param("rid") String rid,
                @Param("bagType") byte bagType,
                @Param("capacity") int capacity);
}