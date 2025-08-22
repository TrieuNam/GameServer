package com.SouthMillion.role_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service")
public interface ConfigFeign {

    // Dùng property để trỏ tới endpoint (có thể là /config/{name}.json hoặc /config/by-path?p=...)
    @GetMapping("${role.config.roleexp-path}")
    ResponseEntity<byte[]> getRoleExp(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    @GetMapping("${role.config.rolename-path}")
    ResponseEntity<byte[]> getRoleName(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    @GetMapping("${role.config.bagcfg-path}")
    ResponseEntity<byte[]> getBagCfg(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    @GetMapping("${role.config.keyconfig-path}")
    ResponseEntity<byte[]> getKeyCfg(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    @GetMapping("${role.config.otherconfig-path}")
    ResponseEntity<byte[]> getOtherCfg(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    // Generic (có hỗ trợ force)
    @GetMapping("/config/{name}.json")
    ResponseEntity<byte[]> getJsonByName(
            @PathVariable("name") String name,
            @RequestParam(value = "force", required = false) Integer force,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    @GetMapping("/config/by-path")
    ResponseEntity<byte[]> getByPath(
            @RequestParam("p") String relPath,
            @RequestParam(value = "force", required = false) Integer force,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );
}