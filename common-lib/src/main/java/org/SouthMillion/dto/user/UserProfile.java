package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfile {
    private String userId;
    private String username;
    private String status;
    private String spid;
    private String externalId;
    private String device;
    private long createdAtEpoch;
    private Long lastLoginAtEpoch;
}