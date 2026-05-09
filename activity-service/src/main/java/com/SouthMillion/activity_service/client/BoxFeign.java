package com.SouthMillion.activity_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@FeignClient(name = "box-service", path = "/api/box", contextId = "activityBoxFeign", fallback = BoxFeign.BoxFeignFallback.class)
@Validated
public interface BoxFeign {

	@GetMapping("/bulkInfo")
	@CircuitBreaker(name = "boxFeignCircuitBreaker", fallbackMethod = "bulkInfoFallback")
	List<BoxDTOs.InfoResp> bulkInfo(
		@RequestBody
		@NotNull(message = "roleIds must not be null")
		List<@Min(value = 1, message = "roleId must be greater than 0") Long> roleIds
	);

	default List<BoxDTOs.InfoResp> bulkInfoFallback(List<Long> roleIds, Throwable throwable) {
		return List.of();
	}

	@Component
	class BoxFeignFallback implements BoxFeign {
		@Override
		public List<BoxDTOs.InfoResp> bulkInfo(List<Long> roleIds) {
			return List.of();
		}
	}
}