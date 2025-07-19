package org.SouthMillion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonsterGroupDTO {
    @JsonProperty("monster_group_id")
    private String monsterGroupId;

    private String name;

    @JsonProperty("monster_id_0")
    private String monsterId0;
    @JsonProperty("monster_id_1")
    private String monsterId1;
    @JsonProperty("monster_id_2")
    private String monsterId2;

    public List<String> getMonsterIds() {
        List<String> ids = new ArrayList<>();
        if (monsterId0 != null && !monsterId0.isEmpty()) ids.add(monsterId0);
        if (monsterId1 != null && !monsterId1.isEmpty()) ids.add(monsterId1);
        if (monsterId2 != null && !monsterId2.isEmpty()) ids.add(monsterId2);
        return ids;
    }
}