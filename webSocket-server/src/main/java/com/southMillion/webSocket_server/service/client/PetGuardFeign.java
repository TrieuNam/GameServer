package com.SouthMillion.webSocket_server.service.client;

// import PetGuardState đã bị xoá vì không còn entity ở webSocket-server.
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pet-service")
public interface PetGuardFeign {
    @GetMapping("/api/petguard/state/{roleId}")
    Object getState(@PathVariable("roleId") Long roleId); // TODO: Định nghĩa DTO hoặc Map phù hợp với response từ pet-service

    @PostMapping("/api/petguard/state")
    void saveState(@RequestBody Object state); // TODO: Định nghĩa DTO hoặc Map phù hợp với request gửi sang pet-service
}
