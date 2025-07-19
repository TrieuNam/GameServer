package com.SouthMillion.user_service.utils;

public class PaymentVerifier {
    public static boolean verify(String transactionId, Long userId, int amount) {
        // Thực tế: kiểm tra transactionId với payment gateway (Google/Apple/OnePay...)
        return transactionId != null && transactionId.length() > 8;
    }
}
