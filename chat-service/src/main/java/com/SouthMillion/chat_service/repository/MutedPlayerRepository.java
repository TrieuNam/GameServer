package com.SouthMillion.chat_service.repository;

import com.SouthMillion.chat_service.entity.MutedPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MutedPlayerRepository extends JpaRepository<MutedPlayer, Long> {

    Optional<MutedPlayer> findByRoleIdAndMuteUntilAfter(Long roleId, LocalDateTime now);
    
    boolean existsByRoleIdAndMuteUntilAfter(Long roleId, LocalDateTime now);
    
    void deleteByMuteUntilBefore(LocalDateTime now);
}
