package org.SouthMillion.dto.session;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DisconnectNoticeDTO {
    private Integer reason;
    private Integer roleId;
    private String userName;
}