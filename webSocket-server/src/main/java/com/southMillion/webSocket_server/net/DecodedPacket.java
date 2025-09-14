package com.southMillion.webSocket_server.net;

public record DecodedPacket(int msgId, byte[] payload) {}