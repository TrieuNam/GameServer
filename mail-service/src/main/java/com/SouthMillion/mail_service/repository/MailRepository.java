package com.SouthMillion.mail_service.repository;

import com.SouthMillion.mail_service.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {

    List<Mail> findByReceiverIdAndIsDeletedFalseOrderByCreatedAtDesc(Long receiverId);

    List<Mail> findByReceiverIdAndIsReadFalseAndIsDeletedFalse(Long receiverId);

    @Query("SELECT COUNT(m) FROM Mail m WHERE m.receiverId = :receiverId AND m.isRead = false AND m.isDeleted = false")
    Integer countUnreadMails(@Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Mail m WHERE m.receiverId = :receiverId AND m.isClaimedAttachment = false AND m.isDeleted = false")
    List<Mail> findUnclaimedMails(@Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Mail m WHERE m.receiverId = :receiverId AND m.isRead = true AND m.isDeleted = false")
    List<Mail> findReadMails(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE Mail m SET m.isDeleted = true WHERE m.expiresAt < :now")
    void deleteExpiredMails(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Mail m WHERE m.isDeleted = true AND m.createdAt < :cutoffTime")
    void permanentlyDeleteOldMails(@Param("cutoffTime") LocalDateTime cutoffTime);
}
