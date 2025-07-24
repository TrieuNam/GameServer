package org.SouthMillion.dto.globalserver;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailEvent {
    private Long userId;
    private Integer mailId;
    private String action; // "FETCH", "DELETE", "READ", ...
    private Long timestamp;
}
