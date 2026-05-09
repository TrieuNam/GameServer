package com.SouthMillion.activity_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import java.util.List;

@FeignClient(name = "bag-service", path = "/api/bag", contextId = "activityBagFeign")
public interface BagFeign {

	@Retryable(
		value = { Exception.class },
		maxAttempts = 3,
		backoff = @Backoff(delay = 2000, multiplier = 2)
	)
	@PostMapping("/grant")
	List<BagDTOs.ItemView> grantItems(@Valid @RequestBody BagDTOs.GrantReq request);
}