package org.SouthMillion.dto.role.other;

import java.time.Instant;
import java.util.List;

public class OtherRoleDTOs {
    public record OtherRoleAttr(
            int level, long exp, long hp, long attackValue, long defenseValue, int speed
    ) {}

    public record OtherRoleAttrPair(int attrType, long attrValue) {}

    public record OtherRoleInfo(
            String userId, String roleId, String name, String headChar, String guildName,
            OtherRoleAttr attributes, List<OtherRoleAttrPair> roleAttrList, List<String> equipment, List<String> pets,
            Long capability, Instant createdAt, Instant updatedAt
    ) {}
}
