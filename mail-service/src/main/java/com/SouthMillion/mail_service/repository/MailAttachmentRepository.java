package com.SouthMillion.mail_service.repository;

import com.SouthMillion.mail_service.entity.MailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MailAttachmentRepository extends JpaRepository<MailAttachment, Long> {

    List<MailAttachment> findByMailId(Long mailId);

    void deleteByMailId(Long mailId);

    boolean existsByMailId(Long mailId);

    /**
     * Batch find attachments by mail IDs (for N+1 query optimization)
     */
    List<MailAttachment> findByMailIdIn(List<Long> mailIds);
}
