package com.SouthMillion.drop_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "${app.config.serviceName}", path = "/config")
public interface ConfigFeign {

    @GetMapping("/version")
    Map<String, Object> version();

    // /config/list/{cat}?offset=...&limit=...
    @GetMapping("/list/{cat}")
    List<String> list(@PathVariable("cat") String cat,
                      @RequestParam(value = "offset", defaultValue = "0") int offset,
                      @RequestParam(value = "limit",  defaultValue = "10000") int limit);

    // /config/gameworld/drop/{id}
    @GetMapping("/gameworld/drop/{id}")
    ResponseEntity<byte[]> drop(@PathVariable("id") String fileLeaf,
                                @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);
}