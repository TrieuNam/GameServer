package com.SouthMillion.bag_service.service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="config-service", path="/config")
public interface ConfigFeign {
    @GetMapping("/gameworld/logic/{feature}")
    ResponseEntity<byte[]> getLogic(@PathVariable("feature") String feature,
                                    @RequestHeader(value="If-None-Match", required=false) String ifNoneMatch);
}