package org.SouthMillion.dto.session;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionDto {
    private String sessionId;
    private String roleId;
    private Long lastActive;
}
