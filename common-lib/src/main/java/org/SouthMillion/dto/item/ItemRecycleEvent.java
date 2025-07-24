package org.SouthMillion.dto.item;

import lombok.*;

import java.util.List;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemRecycleEvent {
    private Long userId;
    private List<Integer> itemIds;
    // getters/setters/constructor
}