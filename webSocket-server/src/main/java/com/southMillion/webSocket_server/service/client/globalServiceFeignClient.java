package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.globalserver.MailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "globalserver-service")
public interface globalServiceFeignClient {

    @GetMapping("/notice/num")
    Integer getUnreadNoticeNum(@RequestParam String userKey);

    @GetMapping("/api/mail/list/{userId}")
    List<MailDTO> getMailList(@PathVariable("userId") Long userId);

    @PostMapping("/api/mail/fetch/{userId}/{mailId}")
    void fetchMail(@PathVariable("userId") Long userId, @PathVariable("mailId") Integer mailId);


}
