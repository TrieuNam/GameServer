package org.SouthMillion.dto.role.other;

import java.time.Instant;
import java.util.List;

public class OtherRoleDTOs {
    public record OtherRoleAttr(
            int level, long exp, long hp, long attackValue, long defenseValue, int speed
    ) {}
    public record OtherRoleInfo(
            String userId, String roleId, String name, String headChar, String guildName,
            OtherRoleAttr attributes, List<String> equipment, List<String> pets,
            Instant createdAt, Instant updatedAt
    ) {}
}
