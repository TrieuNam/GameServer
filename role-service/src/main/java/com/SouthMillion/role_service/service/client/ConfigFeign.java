package com.SouthMillion.role_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "config-service")
public interface ConfigFeign {

    @GetMapping("${role.config.roleexp-path-API}")
    ResponseEntity<byte[]> getRoleExp(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    @GetMapping("${role.config.rolename-path-API}")
    ResponseEntity<byte[]> getRoleName(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    @GetMapping("${role.config.otherconfig-path-API}")
    ResponseEntity<byte[]> getOtherCfg(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    @GetMapping("${role.config.keyconfig-path-API}")
    ResponseEntity<byte[]> getKeyCfg(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch);

    @RequestMapping(method = RequestMethod.HEAD, value = "/api/config/file")
    ResponseEntity<Void> headFile(@RequestParam("path") String path);
}