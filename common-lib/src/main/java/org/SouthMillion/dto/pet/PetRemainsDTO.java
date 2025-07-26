package org.SouthMillion.dto.pet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.proto.Msgpet.Msgpet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetRemainsDTO {
    private Integer seq;
    private Integer grade;
    private Integer level;
    private Integer exp;
    private Integer clothId;
    private Integer index;

    // Mapping to Proto
    public Msgpet.PB_SCPetRemainsNode toProto() {
        return Msgpet.PB_SCPetRemainsNode.newBuilder()
                .setSeq(seq == null ? 0 : seq)
                .setGrade(grade == null ? 0 : grade)
                .setLevel(level == null ? 0 : level)
                .setExp(exp == null ? 0 : exp)
                .setClothId(clothId == null ? 0 : clothId)
                .setIndex(index == null ? 0 : index)
                .build();
    }


}