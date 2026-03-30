package com.SouthMillion.notification_service.repository;

import com.SouthMillion.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByPlayerIdAndStatus(Long playerId, String status);
    
    List<Notification> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
    
    List<Notification> findByStatus(String status);
}
