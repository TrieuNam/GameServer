package org.SouthMillion.dto.globalserver;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
public class KnightsEvent {
    private Long userId;
    private Integer opType;
    private Integer param1;
    // getters/setters/constructor
}
