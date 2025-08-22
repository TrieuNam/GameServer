package com.SouthMillion.item_service.service.client;

import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "config-service", path = "/config")
public interface ConfigFeign {

    // /config/list/item -> trả về danh sách leaf của item (không cần .json)
    @GetMapping("/list/{cat}")
    List<String> list(@PathVariable("cat") String cat,
                      @RequestParam(name = "offset", defaultValue = "0") int offset,
                      @RequestParam(name = "limit", defaultValue = "200") int limit);

    // /config/gameworld/item/{name}
    @GetMapping("/gameworld/item/{name}")
    ResponseEntity<byte[]> getItem(@PathVariable("name") String name,
                                   @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    // /config/gameworld/logic/{*feature}   (rất quan trọng: encoded=true để giữ dấu '/')
    @GetMapping("/gameworld/logic/{*feature}")
    ResponseEntity<byte[]> getLogic(@PathVariable(value = "feature") String feature,
                                    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    // Tối ưu: lấy nhiều file 1 lượt /config/bundle?keys=k1,k2,...
    @GetMapping("/bundle")
    ResponseEntity<List<ConfigEnvelope<String>>> bundle(@RequestParam("keys") String keysCsv);
}