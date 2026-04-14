package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FriendInviteShareProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendInviteShareProgressRepository extends JpaRepository<FriendInviteShareProgress, Long> {
    boolean existsByInviterRoleIdAndInvitedRoleId(Long inviterRoleId, Long invitedRoleId);
}
