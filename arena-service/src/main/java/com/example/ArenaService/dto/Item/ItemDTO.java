package com.example.ArenaService.dto.Item;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ItemDTO {
    @JsonProperty("item_id")
    private String itemId;
    @JsonProperty("num")
    private String num;
    // getter/setter
}