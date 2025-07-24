package com.SouthMillion.globalserver_service.repository;

import com.SouthMillion.globalserver_service.entity.NoticeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeUserRepository extends JpaRepository<NoticeUser, Long> {
    @Query("SELECT COUNT(nu) FROM NoticeUser nu WHERE nu.userKey = :userKey AND nu.isRead = false")
    int countUnread(@Param("userKey") String userKey);

    NoticeUser findByUserKeyAndNoticeId(String userKey, Long noticeId);
}
