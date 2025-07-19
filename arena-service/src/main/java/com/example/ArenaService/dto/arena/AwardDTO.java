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
public class AwardDTO {
    @JsonProperty("seq")
    private String seq;
    @JsonProperty("paihang_1")
    private String paihang1;
    @JsonProperty("paihang_2")
    private String paihang2;
    @JsonProperty("item_list")
    private List<ItemDTO> itemList;
    // getter/setter
}
