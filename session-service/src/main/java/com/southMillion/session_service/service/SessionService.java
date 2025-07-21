package com.southMillion.session_service.service;

import com.southMillion.session_service.entity.SessionHistory;
import com.southMillion.session_service.repository.SessionHistoryRepository;
import com.southMillion.session_service.repository.SessionRepository;
import org.SouthMillion.dto.session.SessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionService {
    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionHistoryRepository historyRepository;

    public void heartbeat(String sessionId, String roleId) {
        SessionDto session = sessionRepository.findBySessionId(sessionId);
        long now = System.currentTimeMillis() / 1000;
        if (session == null) {
            session = new SessionDto(sessionId, roleId, now);
            // Ghi nhận login event vào DB
            SessionHistory his = new SessionHistory();
            his.setRoleId(roleId);
            his.setSessionId(sessionId);
            his.setLoginTime(Instant.now());
            historyRepository.save(his);
        } else {
            session.setLastActive(now);
        }
        session.setRoleId(roleId);
        sessionRepository.save(session);
    }

    public void removeSession(String sessionId) {
        SessionDto session = sessionRepository.findBySessionId(sessionId);
        if (session != null) {
            // Ghi nhận logout vào DB
            SessionHistory his = historyRepository.findTopByRoleIdAndSessionIdOrderByLoginTimeDesc(session.getRoleId(), session.getSessionId());
            if (his != null && his.getLogoutTime() == null) {
                his.setLogoutTime(Instant.now());
                historyRepository.save(his);
            }
        }
        sessionRepository.remove(sessionId);
    }

    public SessionDto getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    public boolean isOnline(Integer roleId) {
        SessionDto session = sessionRepository.findByRoleId(roleId);
        if (session == null) return false;
        long now = System.currentTimeMillis() / 1000;
        return (now - session.getLastActive() <= 60);
    }

    public SessionDto getSessionByRoleId(Integer roleId) {
        return sessionRepository.findByRoleId(roleId);
    }
}