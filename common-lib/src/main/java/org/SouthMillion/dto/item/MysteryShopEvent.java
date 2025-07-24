package org.SouthMillion.dto.item;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MysteryShopEvent {
    private Long userId;
    private Integer itemIndex;
    private Integer num;
    private String action;
    // getters/setters/constructor
}