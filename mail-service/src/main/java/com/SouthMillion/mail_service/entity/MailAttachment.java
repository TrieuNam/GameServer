package com.SouthMillion.mail_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mail Attachment Entity
 * 
 * Attachment Types:
 * 1 = Gold
 * 2 = Gems
 * 3 = Item
 * 4 = Equipment
 * 5 = EXP
 */
@Entity
@Table(name = "mail_attachment", indexes = {
    @Index(name = "idx_attachment_mail", columnList = "mailId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mailId;

    @Column(nullable = false)
    private Integer attachmentType; // 1=Gold, 2=Gems, 3=Item, 4=Equipment, 5=EXP

    @Column(length = 50)
    private String itemId; // Item/Equipment ID

    @Column(length = 100)
    private String itemName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column
    private Integer quality; // Item quality/rarity

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isGold() {
        return attachmentType == 1;
    }

    public boolean isGems() {
        return attachmentType == 2;
    }

    public boolean isItem() {
        return attachmentType == 3 || attachmentType == 4;
    }

    public boolean isExp() {
        return attachmentType == 5;
    }
}
