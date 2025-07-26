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
    private Integer mailIndex;
    private String action; // READ, FETCH, DELETE, ...
}
