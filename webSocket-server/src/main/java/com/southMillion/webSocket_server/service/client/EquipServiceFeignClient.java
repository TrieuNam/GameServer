package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.item.EquipDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "equip-service")
public interface EquipServiceFeignClient {

    @GetMapping("/api/equip/get")
    EquipDto getEquip(@RequestParam("userId") String userId, @RequestParam("equipType") int equipType);

    @GetMapping("/api/equip/list")
    List<EquipDto> getAllEquip(@RequestParam("userId") String userId);

    @PostMapping("/api/equip/update")
    EquipDto addOrUpdateEquip(@RequestBody EquipDto dto);

    @DeleteMapping("/api/equip/delete")
    void deleteEquip(@RequestParam("userId") String userId, @RequestParam("equipType") int equipType);
}
