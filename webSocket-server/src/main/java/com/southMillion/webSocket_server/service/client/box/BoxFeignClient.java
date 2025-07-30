package com.southMillion.webSocket_server.service.client.box;

import org.SouthMillion.dto.item.Box.BoxInfoDTO;
import org.SouthMillion.dto.item.Box.BoxOpenRequestDTO;
import org.SouthMillion.dto.item.Box.BoxRecordDTO;
import org.SouthMillion.dto.item.Box.BoxSetDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-service", contextId = "boxFeignClient")
public interface BoxFeignClient {

    @PostMapping("/api/box/open")
    BoxRecordDTO openBox(@RequestBody BoxOpenRequestDTO req);

    @PostMapping("/api/box/auto-set")
    void saveBoxSetting(@RequestParam("userId") Long userId, @RequestBody BoxSetDTO setting);

    @GetMapping("/api/box/auto-get")
    BoxSetDTO getBoxSetting(@RequestParam("userId") Long userId);

    @GetMapping("/api/box/last-info")
    BoxRecordDTO getLastBoxInfo(@RequestParam("userId") Long userId);

    @PostMapping("/api/box/sell")
    BoxRecordDTO sellBoxItem(@RequestParam("userId") Long userId, @RequestParam("boxId") Integer boxId);

    @PostMapping("/api/box/handle-request")
    BoxInfoDTO handleBoxRequest(@RequestParam("userId") Long userId,
                                @RequestParam("reqType") int reqType,
                                @RequestParam("param") int param);
}
