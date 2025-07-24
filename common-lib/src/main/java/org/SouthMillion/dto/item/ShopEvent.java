package org.SouthMillion.dto.item;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ShopEvent {
    private Long userId;
    private Integer itemIndex;
    private Integer num;
    private String action; // "BUY"
    // getters/setters/constructor
}