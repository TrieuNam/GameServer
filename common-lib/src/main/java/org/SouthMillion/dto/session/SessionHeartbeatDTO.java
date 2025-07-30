package org.SouthMillion.dto.session;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionHeartbeatDTO {
    private Integer reserve;
}
