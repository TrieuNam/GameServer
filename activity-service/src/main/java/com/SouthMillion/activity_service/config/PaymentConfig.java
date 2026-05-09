package com.SouthMillion.activity_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Value("${payment.secret}")
    private String paymentSecret;

    @Value("${payment.apiKey}")
    private String paymentApiKey;

    public String getPaymentSecret() {
        return paymentSecret;
    }

    public String getPaymentApiKey() {
        return paymentApiKey;
    }
}