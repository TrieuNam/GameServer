package com.SouthMillion.activity_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;

@FeignClient(name = "wallet-service", url = "${wallet.service.url}")
public interface WalletFeign {

    @PostMapping(value = "/wallet/debit", consumes = MediaType.APPLICATION_JSON_VALUE)
    void debit(@RequestBody DebitRequest request);

    @PostMapping(value = "/wallet/credit", consumes = MediaType.APPLICATION_JSON_VALUE)
    void credit(@RequestBody CreditRequest request);

    // DTOs
    class DebitRequest {
        private Long userId;
        private Double amount;
        // getters/setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    class CreditRequest {
        private Long userId;
        private Double amount;
        // getters/setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }
}