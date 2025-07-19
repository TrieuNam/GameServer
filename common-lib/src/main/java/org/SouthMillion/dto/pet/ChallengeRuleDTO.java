package org.SouthMillion.dto.pet;

import lombok.Data;

@Data
public class ChallengeRuleDTO {
    private int rule_id;
    private String rule_desc;
    private int rule_value;
    private int rule_type;
}
