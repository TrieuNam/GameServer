package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.BagEventDedup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;


public interface BagEventDedupRepository extends JpaRepository<BagEventDedup, String> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO bag_event_dedup(event_id) VALUES (?1)", nativeQuery = true)
    int doInsertIgnore(String eventId);

    @Transactional
    default boolean insertIgnore(String eventId) {
        return doInsertIgnore(eventId) > 0; // true = lần đầu, false = đã xử lý
    }
}