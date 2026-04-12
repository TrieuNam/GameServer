package com.SouthMillion.webSocket_server.service.grpc;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PickupItemResponse {
    private final boolean success;

    @Builder.Default
    private final int itemId = 0;

    @Builder.Default
    private final int quantity = 0;

    @Builder.Default
    private final String errorCode = "";

    @Builder.Default
    private final String errorMessage = "";

    @Builder.Default
    private final boolean isBagGranted = false;

    public boolean getSuccess() {
        return success;
    }
}
