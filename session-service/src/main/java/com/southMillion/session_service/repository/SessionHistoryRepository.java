package com.southMillion.session_service.repository;

import com.southMillion.session_service.entity.SessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionHistoryRepository extends JpaRepository<SessionHistory, Long> {
    SessionHistory findTopByRoleIdAndSessionIdOrderByLoginTimeDesc(Integer roleId, String sessionId);
}
