package org.SouthMillion.dto.pet;

import lombok.Data;
import org.SouthMillion.dto.pet.Enum.PetBuffType;

@Data
public class PetBuff {
    private PetBuffType type;
    private int value;
    private int turns; // số lượt tồn tại, 0 là vĩnh viễn
}
