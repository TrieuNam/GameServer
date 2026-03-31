package com.SouthMillion.moderation_service.repository;

import com.SouthMillion.moderation_service.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    List<Violation> findByUserIdOrderByCreatedAtDesc(String userId);
    Long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime after);
    List<Violation> findByUserIdAndExpiresAtAfter(String userId, LocalDateTime now);
}
