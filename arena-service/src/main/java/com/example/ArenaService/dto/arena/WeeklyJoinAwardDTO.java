package com.example.ArenaService.dto.arena;

import com.example.ArenaService.dto.Item.ItemDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class WeeklyJoinAwardDTO {
    @JsonProperty("seq")
    private String seq;
    @JsonProperty("num")
    private String num;
    @JsonProperty("item_list")
    private List<ItemDTO> itemList;
    // getter/setter
}
