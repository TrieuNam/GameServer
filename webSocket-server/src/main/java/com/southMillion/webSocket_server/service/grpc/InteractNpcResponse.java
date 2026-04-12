package com.SouthMillion.webSocket_server.service.grpc;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InteractNpcResponse {
    private final boolean success;

    @Builder.Default
    private final String npcType = "UNKNOWN";

    @Builder.Default
    private final String errorCode = "";

    @Builder.Default
    private final String errorMessage = "";

    public boolean getSuccess() {
        return success;
    }
}
