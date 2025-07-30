package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.BoxRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoxRecordRepository extends JpaRepository<BoxRecord, Long> {
    List<BoxRecord> findByUserId(Long userId);
    BoxRecord findTopByUserIdOrderByTimestampDesc(Long userId);
}
