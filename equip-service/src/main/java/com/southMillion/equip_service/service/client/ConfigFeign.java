package com.southMillion.equip_service.service.client;

import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name="config-service", path="/config")
public interface ConfigFeign {
    // lấy file item/equipment.json
    @GetMapping("/gameworld/item/{name}")
    ResponseEntity<byte[]> getItem(@PathVariable("name") String name,
                                       @RequestHeader(value="If-None-Match", required=false) String ifNoneMatch);

    // nếu muốn dùng thêm logicconfig.zip/… (không bắt buộc ở bản core này)
    @GetMapping("/gameworld/logic/{*feature}")
    ResponseEntity<byte[]> getLogic(@PathVariable("feature") String feature,
                                    @RequestHeader(value="If-None-Match", required=false) String ifNoneMatch);

}
