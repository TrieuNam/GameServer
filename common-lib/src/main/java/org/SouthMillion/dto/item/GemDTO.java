package org.SouthMillion.dto.item;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class GemDTO {
    private Integer drawingId;
    private List<GemInlayDTO> gemList;
    // getters/setters
}