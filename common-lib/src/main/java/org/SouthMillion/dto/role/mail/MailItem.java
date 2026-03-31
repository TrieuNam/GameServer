package org.SouthMillion.dto.role.mail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailItem {
    private String itemId; // e.g. GOLD, DIAMOND, POTION
    private long count;
    private String type; // e.g. CURRENCY, ITEM
}