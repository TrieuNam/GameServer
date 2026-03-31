package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MailRepository extends JpaRepository<Mail, String> {
    @Query("select m from Mail m where m.userId = :userId and (m.expireAt is null or m.expireAt > :now) order by m.createdAt desc")
    List<Mail> findActiveByUserId(@Param("userId") String userId, @Param("now") Instant now);

    Optional<Mail> findByMailIdAndUserId(String mailId, String userId);
}
