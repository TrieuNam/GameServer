package com.SouthMillion.box_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "${app.config.serviceName}", path="/config")
public interface ConfigFeign {

    @GetMapping("/by-path")
    ResponseEntity<byte[]> byPath(
            @RequestParam("p") String rel,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestParam(value = "force", required = false, defaultValue = "0") int force
    );
    // Tiện ích riêng cho item/equipment nếu cần:
    @GetMapping("/gameworld/item/{name}")
    ResponseEntity<byte[]> getItem(@PathVariable("name") String name,
                                   @RequestHeader(value="If-None-Match", required=false) String ifNoneMatch);

    @GetMapping("/gameworld/item/gift.json")
    Map<String, Object> giftJson(); // Jackson map thẳng JSON
}