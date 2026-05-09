package com.SouthMillion.activity_service.client;

import org.springframework.stereotype.Component;

@Component
public class WalletFeignFallback implements WalletFeign {

    @Override
    public void debit(DebitRequest request) {
        // Fallback no-op: caller should handle downstream wallet failure at business layer.
    }

    @Override
    public void credit(CreditRequest request) {
        // Fallback no-op: caller should handle downstream wallet failure at business layer.
    }
}