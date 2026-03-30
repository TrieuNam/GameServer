package com.SouthMillion.main_fb_service.service.client;

import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "task-service")
public interface TaskReportFeignClient {

    @PostMapping("/api/task/report")
    void report(@RequestBody TaskReportReq req);
}
