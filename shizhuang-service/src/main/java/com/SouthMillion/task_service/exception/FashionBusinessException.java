package com.SouthMillion.task_service.exception;

import lombok.Getter;

@Getter
public class FashionBusinessException extends RuntimeException {
    private final String code;
    private final Integer itemId;

    public FashionBusinessException(String code, String message, Integer itemId) {
        super(message);
        this.code = code;
        this.itemId = itemId;
    }

    public static FashionBusinessException invalidRequest(String message) {
        return new FashionBusinessException(FashionErrorCodes.INVALID_REQUEST, message, null);
    }

    public static FashionBusinessException notEnoughItem(int itemId, String message) {
        return new FashionBusinessException(FashionErrorCodes.NOT_ENOUGH_ITEM, message, itemId);
    }

    public static FashionBusinessException notEnoughCurrency(int itemId, String message) {
        return new FashionBusinessException(FashionErrorCodes.NOT_ENOUGH_CURRENCY, message, itemId);
    }
}
