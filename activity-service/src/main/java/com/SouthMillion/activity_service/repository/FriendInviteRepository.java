package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FriendInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendInviteRepository extends JpaRepository<FriendInvite, Long> {
    Optional<FriendInvite> findByRoleId(Long roleId);
}
