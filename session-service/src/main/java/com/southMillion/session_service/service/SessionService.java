package com.southMillion.session_service.service;

import com.southMillion.session_service.entity.SessionEntity;
import com.southMillion.session_service.entity.SessionHistory;
import com.southMillion.session_service.repository.SessionHistoryRepository;
import com.southMillion.session_service.config.SessionRedits;
import com.southMillion.session_service.repository.SessionRepository;
import org.SouthMillion.dto.session.DisconnectNoticeDTO;
import org.SouthMillion.dto.session.SessionDto;
import org.SouthMillion.dto.session.TimeAckDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionService {

    @Autowired
    private SessionRepository repo;

    @Autowired
    private SessionRedits sessionRepository;

    @Autowired
    private SessionHistoryRepository historyRepository;


    public void updateHeartbeat(String sessionId, Integer roleId) {
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

    public TimeAckDTO getTimeAck() {
        TimeAckDTO dto = new TimeAckDTO();
        dto.setServerTime(System.currentTimeMillis() / 1000);
        dto.setServerRealStartTime(1720000000L);  // mock
        dto.setOpenDays(100);
        dto.setServerRealCombineTime(1720000000L);
        return dto;
    }

    public DisconnectNoticeDTO getDisconnectInfo(Long userId) {
        DisconnectNoticeDTO dto = new DisconnectNoticeDTO();
        dto.setReason(1);
        dto.setRoleId(1234);
        dto.setUserName("DemoUser");
        return dto;
    }

}